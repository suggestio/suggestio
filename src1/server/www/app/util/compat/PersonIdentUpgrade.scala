package util.compat

import java.util.concurrent.atomic.AtomicLong

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import io.suggest.es.model.{BulkProcessorListener, EsModel}
import io.suggest.model.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.JMXBase
import io.suggest.util.logs.{MacroLogsImpl, MacroLogsImplLazy}
import javax.inject.Inject
import models.mext.ILoginProvider
import models.usr._
import org.elasticsearch.index.query.QueryBuilders
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.02.19 15:21
  * Description: Обновление MPersonIdent на n2-рельсы.
  */
class PersonIdentUpgrade @Inject() (
                                     emailPwIdents        : EmailPwIdents,
                                     //emailActivations   : EmailActivations,
                                     mExtIdents           : MExtIdents,
                                     mNodes               : MNodes,
                                     esModel              : EsModel,
                                     mPersonIdentModel    : MPersonIdentModel,
                                     injector             : Injector,
                                     implicit val mat     : Materializer,
                                     implicit val ec      : ExecutionContext,
                                   )
  extends MacroLogsImpl
{

  import esModel.api._

  implicit class IdentsSourceOpsExt[T <: MPersonIdent]( val src: Source[T, _] ) {
    def groupByPersonIdFut: Future[Map[String, Seq[T]]] = {
      src
        .toMat( Sink.seq )( Keep.right )
        .run()
        .map( _.groupBy(_.personId) )
    }
  }

  /** Выполнить копирование данных идентов внутрь person-узлов. */
  def doIt(): Future[Long] = {
    lazy val logPrefix = s"doIt():"
    val matchAll = QueryBuilders.matchAllQuery()

    // Собрать карту всех внешних идентов:
    val allExtIdentsFut = {
      import mExtIdents.Implicits._
      mExtIdents
        .source[MExtIdent]( matchAll )
        .groupByPersonIdFut
    }

    // Собрать карту всех email-идентов:
    val allEmailPwIdentsFut = {
      import emailPwIdents.Implicits._
      emailPwIdents
        .source[EmailPwIdent]( matchAll )
        .groupByPersonIdFut
    }

    // Все иденты email-активаций не происходит - там наверняка только мусор.
    /*
    val allEmailActIdentsFut = {
      import emailActivations.Implicits._
      emailActivations
        .source[EmailActivation]( matchAll )
        .groupByPersonIdFut
    }
    */

    // Для отработки ext-ident'ов нужно отмаппить логин-провайдеров на MExtServices:
    val loginProvName2ExtServiceMap = ILoginProvider
      .valuesIter
      .map { case (extService, loginProv) =>
        loginProv.ssProvName -> extService
      }
      .toMap

    // Когда всё готово, надо запустить проход по узлам с целью их обновления.
    // Традиционно, сохраняем всё обновлённое назад через BulkProcessor,
    // т.к. наврядли кто-то будет юзера редактировать во время апдейта (да и негде и нечего редактировать сейчас, вобщем-то).
    val bp = mNodes.bulkProcessor(
      listener = new BulkProcessorListener(
        _logPrefix = logPrefix
      )
    )

    val nodesCounter = new AtomicLong(0)

    for {
      // Дождаться сборки всех идентов.
      allExtIdents        <- allExtIdentsFut
      allEmailPwIdents    <- allEmailPwIdentsFut
      //allEmailActIdents   <- allEmailActIdentsFut

      _ <- {
        mNodes
          .source[MNode](
            searchQuery = {
              val msearch = new MNodeSearchDfltImpl {
                override def nodeTypes = MNodeTypes.Person :: Nil
              }
              msearch.toEsQuery
            }
          )( mNodes.Implicits.elSourcingHelper )
          .runForeach { mnode0 =>
            // Найти и отработать все иденты для текущего узла.
            val personId = mnode0.id.get

            // Портирование внешних идентов:
            val extIdentEdges = (for {
              personExtIdents <- allExtIdents.get( personId ).iterator
              personExtIdent <- personExtIdents
              extServiceOpt = loginProvName2ExtServiceMap.get( personExtIdent.provider.ssProvName )
              if extServiceOpt.nonEmpty
            } yield {
              // Основной идент:
              val primaryExtIdentEdge = MEdge(
                predicate = MPredicates.Ident.Id,
                nodeIds = Set( personExtIdent.userId ),
                info = MEdgeInfo(
                  extService = extServiceOpt
                )
              )

              // email-идент, если задан:
              val emailExtIdentEdgeOpt = for {
                email <- personExtIdent.email
              } yield {
                MEdge(
                  predicate = MPredicates.Ident.Email,
                  nodeIds   = Set( email ),
                  info = MEdgeInfo(
                    flag        = Some(true),
                    extService  = extServiceOpt,
                  )
                )
              }

              primaryExtIdentEdge :: emailExtIdentEdgeOpt.toList
            })
              .flatten
              .toStream

            // Портирование emailPw-идентов:
            val pwIdentEdges = (for {
              epwIdents <- allEmailPwIdents.get( personId ).iterator
              epwIdent  <- epwIdents
            } yield {
              val emailEdge = MEdge(
                predicate = MPredicates.Ident.Email,
                nodeIds   = Set(epwIdent.email),
                info = MEdgeInfo(
                  flag      = Some( epwIdent.isVerified )
                )
              )

              val pwEdge = MEdge(
                predicate = MPredicates.Ident.Password,
                info = MEdgeInfo(
                  commentNi = Some(epwIdent.pwHash)
                )
              )

              emailEdge :: pwEdge :: Nil
            })
              .flatten
              .toStream

            // Портирование email activation:
            val identEdges = extIdentEdges #::: pwIdentEdges

            // Если есть ident'ы, то отправить их в сохранение.
            if (identEdges.nonEmpty) {
              val mnode2 = MNode.edges.modify { edges0 =>
                MNodeEdges.out.set(
                  MNodeEdges.edgesToMap1(
                    edges0
                      .withoutPredicateIter( MPredicates.Ident )
                      .++( identEdges )
                  )
                )(edges0)
              }( mnode0 )
              bp.add(
                mNodes.prepareIndex( mnode2 ).request()
              )
              nodesCounter.incrementAndGet()
            }
          }
      }

    } yield {
      bp.close()
      val totalProcessed = nodesCounter.get()
      LOGGER.info(s"$logPrefix Total processed $totalProcessed nodes.")
      totalProcessed
    }
  }

}


trait PersonIdentUpgradeJmxMBean {
  def doIt(): String
}

final class PersonIdentUpgradeJmx @Inject() (
                                              injector                    : Injector,
                                              override implicit val ec    : ExecutionContext,
                                            )
  extends JMXBase
  with PersonIdentUpgradeJmxMBean
  with MacroLogsImplLazy
{

  override def jmxName = "io.suggest:type=compat,name=" + classOf[PersonIdentUpgrade].getSimpleName

  override def doIt(): String = {
    val upgrader = injector.instanceOf[PersonIdentUpgrade]
    val resStr = for (total <- upgrader.doIt()) yield
      s"Done, $total person-nodes updated"
    awaitString( resStr )
  }

}
