package io.suggest.ym.ad

import io.suggest.ym.model.{AdShowLevel, MAdnNode, MAd}
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders}
import org.elasticsearch.client.Client
import io.suggest.util.MacroLogsImpl
import org.elasticsearch.common.xcontent.XContentFactory
import io.suggest.util.SioEsUtil.laFuture2sFuture
import io.suggest.event.{AdSavedEvent, SioNotifierStaticClientI}
import io.suggest.util.MyConfig.CONFIG

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.04.14 9:33
 * Description: Утиль для работы с showLevels, т.е. уровнями отображения рекламы.
 * Эти статические функции в основном относятся к определению возможностей отображения той или иной рекламной карточки
 * у того или иного получателя рекламы.
 *
 * Неявно различаются два вида исходящих количественных лимитов на уровни отображения:
 * - "Максимум 1" или singleton-уровни. У пользователя есть возможность простого переключения того,
 *   какую из карточек отображать.
 *   При активации этого уровня на одной из карточек, на других карточках этот уровень автоматом снимается.
 *   Т.е. чекбоксы этих уровней работают как radiobutton'ы с возможностью отключения.
 * - "Много". Когда достигнут лимит, больше уровней на рекламных карточках включать нельзя.
 *   Пользователю нужно отключить этот уровень на других карточках, чтобы можно было включить на этой карточке.
 *
 * На основе выверенных уровней отображения, возможностей producer'а и receiver'а, формируются
 * публикуемые уровни отображения для конкретного ресивера.
 */
object ShowLevelsUtil extends MacroLogsImpl {

  import LOGGER._

  /** Дефолтовое значение для макс.числа карточек на producer-уровне. */
  val PRODUCER_LEVEL_ADS_COUNT_DFLT = CONFIG.getInt("adn.node.show.levels.lvl_producer.max.dflt") getOrElse 2

  /**
   * Накатить на карточку исходящие трансформации исходных (желаемых) showLevels.
   * На выходе будет список карточек, уровни которых также надо обновить.
   * Список длиной > 2 получается в случае, когда в исходной карточке есть singleton-уровни.
   * Изменений в базе никаких не происходит, чтобы можно было накатить выходные трансформации на ресиверы
   * и затем сохранить.
   * Сохранение результатов происходит через вызов [[updateAllReceivers()]] со списком рекламных карточек.
   * @param thisAd Исходная реклоамная карточка.
   * @param producer Продьюсер.
   * @return Фьчерс с новой рекламной карточкой.
   */
  def applyOutputConstraints(thisAd: MAd, producer: MAdnNode)(implicit ec: ExecutionContext, client: Client): Future[Seq[MAd]] = {
    val adId = thisAd.id.get
    lazy val logPrefix = s"applyOutoutConstraints($adId): "
    val producerId = producer.id.get
    trace(s"${logPrefix}Starting, producer = $producerId / ${producer.meta.name}")
    // Если владелец отключен вообще, то на этом все уровни и заканчиваются.
    val lvlMap = producer.adn.maybeOutShowLevels
    val (levels1, levelsM) = lvlMap.foldLeft[(List[AdShowLevel], List[AdShowLevel])] (Nil -> Nil) {
      case (acc @ (acc1, accM), (asl, v)) =>
        if (v > 1) {
          acc1 -> (asl :: accM)
        } else if (v == 1) {
          (asl :: acc1) -> accM
        } else {
          // Ноль разрешенных карточек на уровне - считаем, что уровня такого нету.
          acc
        }
    }
    // Смотрим, есть ли мульти-уровни в исходящей карточке.
    // Магазин или иной участник сети может отображать только ограниченное кол-во карточек на указанном уровне отображения.
    // В случае использования этих уровней, надо считать текущее кол-во опубликованных карточек на каждом уровне.
    val allowedMLevelsFut: Future[Set[AdShowLevel]] = if (!levelsM.isEmpty) {
      trace(logPrefix + "multi-levels enabled: " + levelsM.mkString(", "))
      val query0 = MAd.producerIdQuery(producerId)
      Future.traverse(levelsM) { lvl =>
        val queryL = MAd.withLevelsFilter(query0, isPub = true, withLevels = Seq(lvl), useAnd = true)
        // Не нужно считать текущий id.
        val noIdFilter = FilterBuilders.notFilter(
          FilterBuilders.termFilter("_id", adId)
        )
        val maxOnLevel = lvlMap(lvl)
        val queryFinal = QueryBuilders.filteredQuery(queryL, noIdFilter)
        MAd.count(queryFinal) map {
          countOnLevel =>
            lvl -> (countOnLevel.toInt < maxOnLevel)
        }
      }.map {
        _.filter(_._2)
          .map(_._1)
          .toSet
      }
    } else {
      Future successful Set.empty
    }

    // С singleton-уровнями всё сложнее. Надо находить текущий уровень среди реалтаймовых карточек, затем заменять
    // уровни в обоих карточках. Для простоты и надежности заменяем уровни во всех карточках сразу.
    val levels1Set = levels1.toSet
    val has1Levels = thisAd.receivers.valuesIterator.exists(!_.slsWant.intersect(levels1Set).isEmpty)
    if (has1Levels) {
      // Вообще все уровни отображения, заявленные в карточке во всех ресиверах.
      val allProdAdsFut = MAd.findForProducerRt(producerId)
      val allAdRcvrsWantSls = thisAd.receivers
        .valuesIterator
        .map { _.slsWant }
        .reduce { _ union _ }
      val ad1Levels = levels1Set intersect allAdRcvrsWantSls
      trace(logPrefix + "Singleton level(s) enabled: " + levels1Set.mkString(", "))
      allProdAdsFut.flatMap { allProdAds =>
        allowedMLevelsFut map { allowedMLevels =>
          allProdAds.foreach { mad =>
            if (mad.idOrNull == adId) {
              // Это текущая карточка. Надо в ней выставить все запрошенные уровни при наличии разрешения на установку оных.
              val thisAdAllowedSls = allowedMLevels ++ ad1Levels   // Все допустимые уровни для текущей карточки.
              // Накатить разрешенные уровни на все перечисленные в карточке ресиверы.
              thisAd.resetReceiversSlsPub(thisAdAllowedSls)
            } else {
              mad.receivers.valuesIterator.foreach { ari =>
                ari.slsPub = ari.slsWant -- ad1Levels
              }
            }
          }
          allProdAds
        }
      }
    } else {
      // singleton-уровней нет в карточке, но фильтровать по третьему уровню её надо. Делаем это...
      allowedMLevelsFut map { allowedMLevels =>
        thisAd.resetReceiversSlsPub(allowedMLevels)
        Seq(thisAd)
      }
    }
  }


  /** Сохранить все значения ресиверов со всех переданных карточек в хранилище модели.
    * Другие поля не будут обновляться. Для ускорения и некоторого подобия транзакционности делаем всё через bulk. */
  def updateAllReceivers(mads: Seq[MAd])(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val bulkRequest = client.prepareBulk()
    mads.foreach { mad =>
      val updReq = mad.updateReceiversReqBuilder
      bulkRequest.add(updReq)
    }
    val resultFut: Future[_] = bulkRequest.execute()
    resultFut onSuccess { case _ =>
      mads.foreach {
        _.emitSavedEvent
      }
    }
    resultFut
  }


  /** Включение продьюсера в рекламную сеть. Нужно пересчитать и перезаписать все pub-уровни.
    * @param producer Продьюсер, для которого произошло переключение isEnabled.
    * @return Фьючерс для синхронизации.
    */
  def handleProducerOnOff(producer: MAdnNode)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val allowedSls = producer.adn.maybeOutShowLevels.keySet
    MAd.findForProducer(producer.id.get) flatMap { prodAds =>
      // Изменяем pub-уровни согласно карте
      prodAds.foreach { mad =>
        mad.resetReceiversSlsPub(allowedSls)
      }
      updateAllReceivers(prodAds)
    }
  }

}

