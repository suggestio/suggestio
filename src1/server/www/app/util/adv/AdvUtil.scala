package util.adv

import java.time.{DayOfWeek, LocalDate}
import io.suggest.ad.blk.{BlockHeights, BlockPaddings, BlockWidths, IBlockSize, IBlockSizes}
import io.suggest.bill._
import io.suggest.bill.price.dsl._
import io.suggest.cal.m.MCalTypes
import io.suggest.common.empty.OptionUtil
import io.suggest.dt.MYmd
import io.suggest.es.model.EsModel
import io.suggest.jd.tags.JdTag
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.util.logs.MacroLogsImpl

import javax.inject.Inject
import models.adv.{IAdvBillCtx, MAdvBillCtx}
import models.mcal.MCalsCtx
import models.mctx.Context
import models.mdt.IDateStartEnd
import play.api.inject.Injector
import scalaz.{EphemeralStream, Tree}
import util.TplDataFormatUtil
import util.billing.TfDailyUtil
import util.cal.CalendarUtil

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 21:57
  * Description: Какая-то очень общая утиль для размещения.
  */
final class AdvUtil @Inject() (
                                injector: Injector,
                              )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val tfDailyUtil = injector.instanceOf[TfDailyUtil]
  private lazy val calendarUtil = injector.instanceOf[CalendarUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  /** Дни недели, относящиеся к выходным. Задаются списком чисел от 1 (пн) до 7 (вс), согласно DateTimeConstants. */
  private def WEEKEND_DAYS: Set[Int] = {
    (DayOfWeek.SATURDAY :: DayOfWeek.SUNDAY :: Nil)
      .iterator
      .map(_.getValue)
      .toSet
  }


  /** Извлечь главный BlockMeta из узла-карточки. */
  def getAdvMainBlock(mad: MNode, blockOnly: Option[Boolean] = None): Option[Tree[JdTag]] = {
    for {
      doc <- mad.extras.doc
      (mainTree, _) <- doc.template.getMainBlockOrFirst( blockOnly )
    } yield {
      mainTree
    }
  }


  /**
    * Высокоуровневый рассчет цены размещения рекламной карточки. Вычисляет кол-во рекламных модулей и дергает
    * другой одноимённый метод.
    *
    * @param mad Рекламная карточка или иная реализация блочного документа.
    * @return Площадь карточки.
    *         NoSuchElementException, если узел не является рекламной карточкой.
    */
  def adModulesCount(mad: MNode): Option[Int] = {
    for {
      mainJdTree  <- getAdvMainBlock( mad, blockOnly = OptionUtil.SomeBool.someTrue )
      props1 = mainJdTree.rootLabel.props1
      widthPx     <- props1.widthPx
      heightPx    <- props1.heightPx
    } yield {
      val outlinePx = BlockPaddings.default.outlinePx

      def __getRelSz(fromPx: Int, model: IBlockSizes[_ <: IBlockSize]): Int = {
        model
          .withValueOpt( fromPx )
          .fold( Math.max( 1, widthPx / (model.min.value + outlinePx) ) )(_.relSz)
      }
      val wmul = __getRelSz( widthPx, BlockWidths )
      val hmul = __getRelSz( heightPx, BlockHeights )

      val result = wmul * hmul
      LOGGER.trace(s"adModulesCount(${mad.id.orNull}) => $result, because width=${widthPx}px=$wmul height=${heightPx}px=$hmul")
      result
    }
  }


  /** Сборка считалки стоимости размещения на указанном ресивере.
    * Это реинкарнация метода calculateAdvPriceOnRcvr(), без кривых листенеров и соответствующего кода.
    *
    * @param tfRcvrId id ресивера.
    * @param abc Контекст рассчётов.
    * @return Для получения цены можно вызвать .price.
    */
  def calcDateAdvPriceOnTf(tfRcvrId: String, abc: IAdvBillCtx): Tree[PriceDsl] = {
    lazy val logPrefix = s"calcDateAdvPriceOnTf($tfRcvrId)[${System.currentTimeMillis}]:"

    // Извлечь подходящий тариф из карты тарифов узлов.
    abc.tfsMap.get(tfRcvrId).fold[Tree[PriceDsl]] {
      // TODO Валюта нулевого ценника берётся с потолка. Нужен более адекватный источник валюты.
      val res = MPrice(0L, MCurrencies.default)
      LOGGER.debug(s"$logPrefix Missing TF for $tfRcvrId. Guessing adv as free: $res")
      Tree.Leaf(
        PriceDsl.base(
          price = res
        )
      )

    } { tf =>

      LOGGER.trace(s"$logPrefix Starting with tf = $tf")

      val dateStart = abc.ivl.dateStart
      val dateEnd = abc.ivl.dateEnd
      // Проверять dateStart <= dateEnd не требуется, т.к. цикл суммирования проверяет это на каждом шаге.
      //assert(!(dateStart isAfter dateEnd), "dateStart must not be after dateEnd")

      // Разбиваем правила tf.clauses на дефолтовое и остальные, привязанные к календарям.
      // По будним (~некалендарным) дням используется это правило:
      val clauseDflt = tf.clauses
        .valuesIterator
        .find(_.calId.isEmpty)
        .getOrElse {
          // Should not happen: посуточный тариф без дефолтового правила
          LOGGER.error(s"$logPrefix WeekDay clause is undefined for $tf. This is a configuration error in rcvr-node.")
          tfDailyUtil.VERY_DEFAULT_WEEKDAY_CLAUSE
        }

      // Собрать правила с календарями для остальных дней. Правил календарных может и не быть вообще.
      val clausesWithCals = tf.clauses
        .valuesIterator
        .flatMap { clause =>
          for {
            calId <- clause.calId
            calCtx <- abc.mcalsCtx.calsMap.get(calId)
          } yield {
            clause -> calCtx
          }
        }
        .toSeq

      // Кешируем тут значение списка выходных, на всякий случай.
      val weekendDays = WEEKEND_DAYS

      // Рассчет стоимости для одной даты (дня) размещения.
      def calculateDateAdvPrice(day: LocalDate): Tree[PriceDsl] = {
        val dayOfWeek = day.getDayOfWeek.getValue

        val clause4dayOpt = clausesWithCals
          .find { case (_, calCtx) =>
            val calExt = calCtx.mcal.extras.calendar.get
            calExt.calType.maybeWeekend(dayOfWeek, weekendDays) || calCtx.mgr.isHoliday(day)
          }

        // Пройтись по праздничным календарям, попытаться найти подходящий
        val clause4day = clause4dayOpt
          .fold(clauseDflt)(_._1)

        LOGGER.trace(s"$logPrefix $day -> ${clause4day.name} +${clause4day.amount} ${tf.currency}")

        val dayAmount = clause4day.amount

        Tree.Leaf(
          PriceDsl.base(
            price     = MPrice(dayAmount, tf.currency),
            mCalType  = clause4dayOpt
              .map(_._2.mcal.extras.calendar.get.calType)
              .orElse( Some(MCalTypes.WeekDay) ),
            date      = Some( MYmd.from(day) )
          )
        )
      }

      // Цикл суммирования стоимости дат, начиная с $1 и заканчивая dateEnd.
      @tailrec def walkDaysAndPrice(day: LocalDate,
                                    accRev0: EphemeralStream[Tree[PriceDsl]] = EphemeralStream.emptyEphemeralStream
                                   ): EphemeralStream[Tree[PriceDsl]] = {
        val accRev1 = calculateDateAdvPrice(day) ##:: accRev0
        val day1 = day.plusDays(1)
        if (!day1.isBefore(dateEnd)) {
          accRev1.reverse
        } else {
          walkDaysAndPrice(day1, accRev1)
        }
      }

      // amount1 - минимальная оплата одного минимального блока по времени
      lazy val amount1 = Tree.Node(
        PriceDsl.sum(),
        walkDaysAndPrice(dateStart)
      )

      // amountN -- amount1 домноженная на кол-во блоков карточки.
      val amountN = abc.blockModulesCount.fold [Tree[PriceDsl]] (amount1) { bmc =>
        Tree.Node(
          PriceDsl.mapper(
            multiplifier = Some(bmc),
            reason       = Some(
              MPriceReason(
                reasonType  = MReasonTypes.BlockModulesCount,
                ints        = bmc :: Nil
              )
            ),
          ),
          amount1 ##:: EphemeralStream.emptyEphemeralStream[Tree[PriceDsl]],
        )

      }

      LOGGER.trace(s"$logPrefix amount (min/full) = ${amount1.price} / ${amountN.price}")
      amountN
    }
  }


  /**
    * Собрать контект rcvr-биллинга для вызова calculateAdvPriceOnRcvr().
    *
    * @param mad Размещаемая рекламная карточка.
    * @param rcvrIds id ресиверов. Желательно, в виде множества без дубликатов.
    * @param ivl Период размещения.
    * @return Фьючерс с готовым к использованию контекстом rcvr-биллинга.
    */
  def rcvrBillCtx(mad: MNode, rcvrIds: Iterable[String], ivl: IDateStartEnd): Future[MAdvBillCtx] = {
    // Посчитать размеры карточки
    rcvrBillCtx(rcvrIds, ivl, bmc = adModulesCount(mad))
  }
  def rcvrBillCtx(rcvrIds: Iterable[String], ivl: IDateStartEnd, bmc: Option[Int]): Future[MAdvBillCtx] = {
    import esModel.api._

    // Собираем все упомянутые узлы.
    val rcvrsFut = mNodes.multiGetCache( rcvrIds )

    // Собираем карту тарифов размещения на узлах.
    val tfsMapFut = rcvrsFut.flatMap( tfDailyUtil.getNodesTfsMap )

    // Оформить собранные ресиверы в карту по id.
    val rcvrsMapFut = for (rcvrs <- rcvrsFut) yield {
      rcvrs
        .zipWithIdIter[String]
        .to( Map )
    }

    // Получить необходимые календари, также составив карту по id
    val calsCtxFut = tfsMapFut.flatMap { tfsMap =>
      val calIds = tfDailyUtil.tfsMap2calIds( tfsMap )
      calendarUtil.getCalsCtx(calIds)
    }

    for {
      tfsMap    <- tfsMapFut
      calsCtx   <- calsCtxFut
      rcvrsMap  <- rcvrsMapFut
    } yield {
      MAdvBillCtx(
        blockModulesCount = bmc,
        mcalsCtx  = calsCtx,
        tfsMap    = tfsMap,
        ivl       = ivl,
        rcvrsMap  = rcvrsMap
      )
    }
  }


  /** Контекст для бесплатного размещения. */
  def freeRcvrBillCtx(mad: MNode, ivl: IDateStartEnd): MAdvBillCtx = {
    MAdvBillCtx(
      blockModulesCount = adModulesCount(mad),
      mcalsCtx          = MCalsCtx.empty,
      tfsMap            = Map.empty,
      ivl               = ivl,
      rcvrsMap          = Map.empty
    )
  }


  /** Подготовить price-терм к показу юзеру на экране.
    *
    * @param priceDsl Исходный терм.
    * @param ctx Контекст рендера.
    * @return Price-терм, готовый к отправке клиенту.
    */
  def prepareForRender(priceDsl: Tree[PriceDsl])(implicit ctx: Context): Tree[PriceDsl] = {
    priceDsl
      .mapAllPrices { TplDataFormatUtil.setFormatPrice }
  }


  /** Подготовить цены к сохранению в mitem или куда-либо ещё.
    * Цены нужно нормализовать.
    *
    * @param priceDsl Нормализуемый price-терм.
    * @return Нормализованный price-терм.
    */
  def prepareForSave(priceDsl: Tree[PriceDsl]): Tree[PriceDsl] = {
    // Раньше тут была нормализация дробной части стоимости, теперь ничего нет. TODO Удалить метод?
    priceDsl
  }

}
