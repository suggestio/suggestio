package util.billing

import models._
import org.joda.time.{Period, DateTime, LocalDate}
import org.joda.time.DateTimeConstants._
import scala.annotation.tailrec
import util.blocks.{BfHeight, BlocksUtil, BlocksConf}
import util.PlayMacroLogsImpl
import play.api.db.DB
import play.api.Play.{current, configuration}
import io.suggest.ym.parsers.Price
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import util.SiowebEsUtil.client
import org.elasticsearch.index.engine.VersionConflictEngineException
import scala.util.{Success, Failure}
import util.event.SiowebNotifier.Implicts.sn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.14 19:04
 * Description: Утиль для работы с биллингом, где имеют вес площади и расценки получателя рекламы.
 */
object MmpDailyBilling extends PlayMacroLogsImpl {

  import LOGGER._

  /** Сколько раз пытаться повторять сохранение обновлённого списка ресиверов. */
  val UPDATE_RCVRS_VSN_CONFLICT_TRY_MAX = configuration.getInt("mmp.daily.save.update.rcvrs.onConflict.try.max") getOrElse 5

  /**
   * Рассчитать ценник размещения рекламной карточки.
   * Цена блока рассчитывается по площади, тарифам размещения узла-получателя и исходя из будней-праздников.
   * @return
   */
  def calculateAdvPrice(blockModulesCount: Int, rcvrPricing: MBillMmpDaily, advTerms: AdvTerms): Price = {
    // Во избежание бесконечного цикла, огораживаем dateStart <= dateEnd
    val dateStart = advTerms.dateStart
    val dateEnd = advTerms.dateEnd
    assert(!dateStart.isAfter(dateEnd), "dateStart must not be after dateEnd")
    def calculateDateAdvPrice(day: LocalDate): Float = {
      val isWeekend = day.getDayOfWeek match {
        case (SUNDAY | SATURDAY) => true
        case _ => false
      }
      if (isWeekend) {
        rcvrPricing.mmpWeekend
      } else {
        rcvrPricing.mmpWeekday
      }
    }
    @tailrec def walkDaysAndPrice(day: LocalDate, acc: Float): Float = {
      val acc1 = calculateDateAdvPrice(day) + acc
      val day1 = day.plusDays(1)
      if (day1 isAfter dateEnd) {
        acc1
      } else {
        walkDaysAndPrice(day1, acc1)
      }
    }
    val amount1 = walkDaysAndPrice(dateStart, 0F)
    var amountAllBlocks: Float = blockModulesCount * amount1
    if (advTerms.onStartPage)
      amountAllBlocks *= rcvrPricing.onStartPage
    Price(amountAllBlocks, rcvrPricing.currency)
  }


  /**
   * Высокоуровневый рассчет цены размещения рекламной карточки. Вычисляет кол-во рекламных модулей и дергает
   * другой одноимённый метод.
   * @param mad Рекламная карточка.
   * @param rcvrPricing Ценовой план получателя.
   * @return Стоимость размещения в валюте получателя.
   */
  def calculateAdvPrice(mad: MAdT, rcvrPricing: MBillMmpDaily, advTerms: AdvTerms): Price = {
    lazy val logPrefix = s"calculateAdvPrice(${mad.id.getOrElse("?")}): "
    val block: BlockConf = BlocksConf(mad.blockMeta.blockId)
    // Мультипликатор по ширине
    val wmul = block.blockWidth match {
      case BlocksUtil.BLOCK_WIDTH_NORMAL_PX => 2
      case BlocksUtil.BLOCK_WIDTH_NARROW_PX => 1
      case other =>
        warn(logPrefix + "Unexpected block width: " + other)
        1
    }
    // Мультипликатор по высоте
    val hmul = mad.blockMeta.height match {
      case BfHeight.HEIGHT_300 => 1
      case BfHeight.HEIGHT_460 => 2
      case BfHeight.HEIGHT_620 => 3
      case other =>
        warn(logPrefix + "Unexpected block height: " + other)
        1
    }
    val blockModulesCount: Int = wmul * hmul
    calculateAdvPrice(blockModulesCount, rcvrPricing, advTerms)
  }


  /** Поиск и размещение в выдачах рекламных карточек, время размещения которых уже пришло. */
  def advertiseOfflineAds() {
    val advs = DB.withConnection { implicit c =>
      MAdvOk.findAllOfflineOnTime
    }
    val logPrefix = "advertiseOfflineAds(): "
    if (!advs.isEmpty) {
      trace(s"${logPrefix}Where are ${advs.size} items. ids = ${advs.map(_.id.get).mkString(", ")}")
      val advsMap = advs.groupBy(_.adId)
      val sls0 = List(AdShowLevels.LVL_MEMBER)
      advsMap foreach { case (adId, advsOk) =>
        // Определяем неблокирующую фунцию обновлению, которую можно многократно вызывать. Повторная попытка обновления поможет разрулить конфликт версий.
        def tryUpdateRcvrs(counter: Int): Future[_] = {
          MAd.getById(adId) map {
            case Some(mad) =>
              mad.receivers ++= advsOk.foldLeft[List[(String, AdReceiverInfo)]](Nil) { (acc, advOk) =>
                trace(s"${logPrefix}Advertising ad $adId on rcvrNode ${advOk.rcvrAdnId}; advOk.id = ${advOk.id.get}")
                val sls = if (advOk.onStartPage) {
                  AdShowLevels.LVL_START_PAGE :: sls0
                } else {
                  sls0
                }
                val slss = sls.toSet
                val rcvrInfo = mad.receivers.get(advOk.rcvrAdnId) match {
                  case None =>
                    AdReceiverInfo(advOk.rcvrAdnId, slss, slss)
                  case Some(ri) =>
                    // Всё уже готово вроде бы.
                    ri.copy(slsWant = ri.slsWant ++ slss, slsPub = ri.slsPub ++ slss)
                }
                advOk.rcvrAdnId -> rcvrInfo :: acc
              }
              // Сохраняем. Нужно отрабатывать ситуацию с изменившейся версией
              mad.saveReceivers
                .recoverWith {
                  case ex: VersionConflictEngineException =>
                    if (counter < UPDATE_RCVRS_VSN_CONFLICT_TRY_MAX)
                      tryUpdateRcvrs(counter + 1)
                    else
                      Future failed new RuntimeException(s"Too many version conflicts: $counter, lastVsn = ${mad.versionOpt}", ex)
                }

            case None =>
              error(s"${logPrefix}MAd not found: $adId, but it should.")
          }
        }
        tryUpdateRcvrs(0) onComplete {
          case Success(_) =>
            trace(s"${logPrefix}Saving online state for ${advsOk.size} advsOk...")
            val now = DateTime.now
            val advsOk1 = advsOk.map { advOk =>
              val dateDiff = new Period(advOk.dateStart, now)
              advOk.copy(
                dateStart = now,
                dateEnd = advOk.dateEnd plus dateDiff,
                isOnline = true
              )
            }
            DB.withTransaction { implicit c =>
              advsOk1.foreach(_.save)
            }
            trace(s"${logPrefix}Successfully onlined ad $adId for advs = ${advsOk.mkString(", ")}")

          case Failure(ex) =>
            error(s"${logPrefix}Failed to make ad $adId online", ex)
        }
      }
    } else {
      trace(logPrefix + "Nothing to do.")
    }
  }

}
