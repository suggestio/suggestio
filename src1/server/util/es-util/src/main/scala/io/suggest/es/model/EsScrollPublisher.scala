package io.suggest.es.model

import javax.inject.Inject
import akka.actor.{Actor, ActorSystem, PoisonPill, Props, Stash}
import com.google.inject.assistedinject.Assisted
import io.suggest.es.util.IEsClient
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.search.SearchHit
import org.reactivestreams.{Publisher, Subscriber, Subscription}
import io.suggest.es.util.SioEsUtil.EsActionBuilderOpsExt
import org.elasticsearch.action.search.SearchResponse

import scala.collection.mutable
import scala.util.{Failure, Success}


/** Guice-factory для сборки инстансов [[EsScrollPublisher]]. */
trait EsScrollPublisherFactory {
  /** mk publisher instance. */
  def publisher(scrollArgs: IScrollArgs): EsScrollPublisher
}

/**
 * An implementation of the reactive API Publisher, that publishes documents using an elasticsearch
 * scroll cursor. The initial query must be provided to the publisher, and there are helpers to create
 * a query for all documents in an index (and type).
 */
class EsScrollPublisher @Inject() (
                                    @Assisted scrollArgs      : IScrollArgs,
                                    scrollSubscriptionFactory : EsScrollSubscriptionFactory
                                  )
  extends Publisher[SearchHit] {

  override def subscribe(s: Subscriber[_ >: SearchHit]): Unit = {
    // Rule 1.9 subscriber cannot be null
    if (s == null) throw new NullPointerException("Rule 1.9: Subscriber cannot be null")
    val subscription = scrollSubscriptionFactory.subsription(scrollArgs, s)
    s.onSubscribe(subscription)
    // rule 1.03 the subscription should not invoke any onNext's until the onSubscribe call has returned
    // even tho the user might call request in the onSubscribe, we can't start sending the results yet.
    // this ready method signals to the actor that its ok to start sending data.
    subscription.ready()
  }

}


/** Guice-factory для сборки инстансов [[EsScrollSubscription]]. */
trait EsScrollSubscriptionFactory {
  def subsription(scrollArgs: IScrollArgs, s: Subscriber[_ >: SearchHit]) : EsScrollSubscription
}

class EsScrollSubscription @Inject() (
                                       @Assisted scrollArgs    : IScrollArgs,
                                       @Assisted s             : Subscriber[_ >: SearchHit],
                                       actorSystem             : ActorSystem,
                                       publishActoryFactory    : EsPublishActoryFactory
                                     )
  extends Subscription {

  private val actor = actorSystem.actorOf {
    Props(
      publishActoryFactory.publishActor(scrollArgs, s)
    )
  }

  private[model] def ready(): Unit = {
    actor ! EsPublishActor.Ready
  }

  override def cancel(): Unit = {
    // Rule 3.5: this call is idempotent, is fast, and thread safe
    // Rule 3.7: after cancelling, further calls should be no-ops, which is handled by the actor
    // we don't mind the subscriber having any pending requests before cancellation is processed
    actor ! PoisonPill
  }

  override def request(n: Long): Unit = {
    // Rule 3.9
    if (n < 1) s.onError(new java.lang.IllegalArgumentException("Rule 3.9: Must request > 0 elements"))
    // Rule 3.4 this method returns quickly as the search request is non-blocking
    actor ! EsPublishActor.Request(n)
  }
}


/** Guice DI-factory для сборки инстансов [[EsPublishActor]]. */
trait EsPublishActoryFactory {
  def publishActor(scrollArgs: IScrollArgs, s: Subscriber[_ >: SearchHit]): EsPublishActor
}


object EsPublishActor {
  object Ready
  sealed case class Request(n: Long)
}


class EsPublishActor @Inject() (
                                 @Assisted scrollArgs  : IScrollArgs,
                                 @Assisted s           : Subscriber[_ >: SearchHit],
                                 esModel               : EsModel,
                                 esClientP             : IEsClient,
                               )
  extends Actor
  with Stash
{

  import context.dispatcher
  import esModel.api._
  import esClientP.esClient

  private var scrollIdOpt: Option[String] = None
  private var processed: Long = 0
  private val queue: mutable.Queue[SearchHit] = mutable.Queue.empty

  private val max = scrollArgs.maxResults.getOrElse( Long.MaxValue )

  // rule 1.03 the subscription should not send any results until the onSubscribe call has returned
  // even tho the user might call request in the onSubscribe, we can't start sending the results yet.
  // this ready method signals to the actor that its ok to start sending data. In the meantime we just stash requests.
  override def receive: PartialFunction[Any, Unit] = {
    case EsPublishActor.Ready =>
      context.become( ready )
      unstashAll()
    case _ =>
      stash()
  }

  private def send(k: Long): Unit = {
    require(queue.size >= k)
    for ( _ <- 0L until k ) {
      if (max == 0 || processed < max) {
        s.onNext(queue.dequeue)
        processed = processed + 1
        if (processed == max && max > 0) {
          s.onComplete()
          context.stop(self)
        }
      }
    }
  }

  private def ready: Actor.Receive = {
    // if a request comes in for more than is currently available,
    // we will send a request for more while sending what we can now
    case EsPublishActor.Request(n) if n > queue.size =>
      scrollIdOpt.fold {
        // Начинается первый запрос.
        val srb0 = scrollArgs.model
          .prepareScroll( scrollArgs.keepAlive )
          .setQuery( scrollArgs.query )
          .setSize( scrollArgs.resultsPerScroll )
        scrollArgs.sourcingHelper
          .prepareSrb(srb0)
          .executeFut()
          .onComplete { result => self ! result }

      } { id =>
        // Продолжается скроллинг.
        esClient.prepareSearchScroll( id )
          .setScroll( scrollArgs.keepAlive )
          .executeFut()
          .onComplete(result => self ! result)
      }
      // we switch state while we're waiting on elasticsearch, so we know not to send another request to ES
      // because we are using a scroll and can only have one active request at at time.
      context.become( fetching )
      // queue up a new request to handle the remaining ones required when the ES response comes in
      self ! EsPublishActor.Request(n - queue.size)
      send(queue.size)

    // in this case, we have enough available so just send 'em
    case EsPublishActor.Request(n) =>
      send(n)
  }


  // fetching state is when we're waiting for a reply from es for a request we sent
  private def fetching: Actor.Receive = {
    // if we're in fetching mode, its because we ran out of results to send
    // so any requests must be stashed until a fresh batch arrives
    case _: EsPublishActor.Request =>
      require(queue.isEmpty) // must be empty or why did we not send it before switching to this mode?
      stash()

    // handle when the es request dies
    case Success(resp: SearchResponse) if resp.isTimedOut =>
      s.onError(new ElasticsearchException("Request terminated early or timed out"))
      context.stop(self)

    // if the request to elastic failed we will terminate the subscription
    case Failure(t) =>
      s.onError(t)
      context.stop(self)

    // if we had no results from ES then we have nothing left to publish and our work here is done
    case Success(resp: SearchResponse) if resp.getHits.getHits.isEmpty =>
      s.onComplete()
      for (scrollIdStr <- scrollIdOpt) {
        esClient
          .prepareClearScroll()
          .addScrollId(scrollIdStr)
          .executeFut()
      }
      context.stop(self)

    // more results and we can unleash the beast (stashed requests) and switch back to ready mode
    case Success(resp: SearchResponse) =>
      scrollIdOpt = Option( resp.getScrollId )
      queue.enqueueAll(resp.getHits.getHits)
      context.become( ready )
      unstashAll()
  }
}
