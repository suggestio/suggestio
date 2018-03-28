package util.adv

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import scalaz._
import scalaz.syntax.apply._
import io.suggest.adv.AdvConstants
import io.suggest.adv.AdvConstants.Su
import io.suggest.adv.free.MAdv4FreeProps
import io.suggest.bill.MGetPriceResp
import io.suggest.common.empty.OptionUtil
import io.suggest.dt.{IPeriodInfo, MAdvPeriod, MYmd, YmdHelpersJvm}
import io.suggest.dt.interval.{MRangeYmd, QuickAdvIsoPeriod, QuickAdvPeriods}
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import models.adv.form._
import models.mctx.Context
import models.req.{IReq, IReqHdr}
import play.api.data.Forms._
import play.api.data.{Form, _}
import util.TplDataFormatUtil

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 20:39
 * Description: Общая утиль для маппингов разных форм размещения рекламной карточки.
 */
@Singleton
class AdvFormUtil @Inject() (
                              dtUtilJvm         : YmdHelpersJvm,
                              implicit private val ec: ExecutionContext
                            ) {

  import dtUtilJvm.Implicits._


  /** Отдельный маппинг для adv-формы, который парсит исходные данные по бесплатному размещению. */
  def freeAdvFormM: Form[Option[Boolean]] = {
    Form(
      Su.ADV_FOR_FREE_FN -> optional(boolean)
    )
  }

  /** Генератор списка шаблонных периодов размещения. */
  def advPeriodsAvailable: IndexedSeq[String] = {
    QuickAdvPeriods.values
      .map(_.value)
  }


  import AdvConstants.DtPeriod._

  /** Маппинг для интервала дат размещения. Его точно нельзя заворачивать в val из-за LocalDate.now(). */
  def advDatePeriodOptM: Mapping[Option[(LocalDate, LocalDate)]] = {
    // option используется, чтобы избежать ошибок маппинга, если галочка isAdv убрана для текущего ресивера, и дата не выставлена одновременно.
    // TODO Неправильно введённые даты надо заворачивать в None.
    val dateOptM = optional( localDate("yyyy-MM-dd") )
    tuple(
      START_FN -> dateOptM
        .verifying("error.date.start.before.today", {dOpt => dOpt match {
          case Some(d)  => !d.isBefore(LocalDate.now)
          case None     => true
        }}),
      END_FN   -> dateOptM
    )
    .transform [Option[(LocalDate, LocalDate)]] (
      {case (Some(dateStart), Some(dateEnd))  =>  Some(dateStart -> dateEnd)
       case _                                 =>  None },
      {case Some((dateStart, dateEnd))        =>  Some(dateStart) -> Some(dateEnd)
       case None                              =>  None -> None }
    )
  }

  /** Форма исповедует select, который имеет набор предустановленных интервалов, а также имеет режим задания дат вручную. */
  def advPeriodM: Mapping[MDatesPeriod] = {
    val custom = QuickAdvPeriods.Custom.value
    tuple(
      QUICK_PERIOD_FN -> nonEmptyText(minLength = 1, maxLength = 10)
        .transform [Option[QuickAdvIsoPeriod]] (
          {periodRaw =>
            if (periodRaw == custom)
              None
            else
              QuickAdvPeriods.withNameOptionIso(periodRaw)
          },
          { _.fold(custom)(_.value) }   // TODO Тут было .isoPeriod, но он тут недоступен, поэтому strId.
        ),
      DATES_INTERVAL_FN -> advDatePeriodOptM
    )
    .verifying("error.required", { m => m match {
      case (periodOpt, datesOpt)  =>  periodOpt.isDefined || datesOpt.isDefined
    }})
    // Проверяем даты у тех, у кого выставлены галочки. end должна быть не позднее start.
    .verifying("error.date.end.before.start", { m => m match {
       // Если даты имеют смысл, то они заданы, и их проверяем.
       case (None, Some((dateStart, dateEnd)))    => !(dateStart isAfter dateEnd)
       // Остальные случаи не отрабатываем - смысла нет.
       case _ => true
    }})

    .transform [MDatesPeriod] (
      // В зависимости от имеющихся значений полей выбираем реальный период.
      { case (Some(qap), _) =>
          MDatesPeriod(qap)
        case (_, dpo) =>
          val (dstart, dend) = dpo.get
          MDatesPeriod(None, dstart, dend)
      },
      // unapply(). Нужно попытаться притянуть имеющийся интервал дат на какой-то период из списка QuickAdvPeriod.
      // При неудаче вернуть кастомный период.
      {dsp =>
        dsp.quickPeriod match {
          case Some(qap)  =>
            val mdp = MDatesPeriod(qap)
            Some(qap) -> Some( (mdp.dateStart, mdp.dateEnd) )
          case None =>
            None -> Some((dsp.dateStart, dsp.dateEnd))
        }
      }
    )
  }


  def maybeFreeAdv()(implicit request: IReq[_]): Boolean = {
    // Раньше было ограничение на размещение с завтрашнего дня, теперь оно снято.
    val isFreeOpt = freeAdvFormM
      .bindFromRequest()
      .fold({_ => None}, identity)
    isFreeAdv( isFreeOpt )
  }

  /** На основе маппинга формы и сессии суперюзера определить, как размещать рекламу:
    * бесплатно инжектить или за деньги размещать. */
  def isFreeAdv(isFreeOpt: Option[Boolean])(implicit request: IReqHdr): Boolean = {
    isFreeOpt.exists { _ && request.user.isSuper }
  }

  /** Значение флага бесплатного размещения суперюзером сконвертить в начальный статус item'а. */
  def suFree2newItemStatus(isFree: Boolean): MItemStatus = {
    if (isFree) {
      // Размещения суперюзеров проходят без участия корзины.
      MItemStatuses.Offline
    } else {
      // Остальные размещения улетают в корзину.
      MItemStatuses.Draft
    }
  }


  /** Нужно здесь отрендерить amount для каждой суммы, т.к. на стороне scala.js это геморно. */
  def prepareAdvPricing(pricing: MGetPriceResp)(implicit ctx: Context): MGetPriceResp = {
    if ( pricing.prices.exists(_.amountStrOpt.isEmpty) ) {
      pricing.withPrices(
        for (mprice <- pricing.prices) yield {
          TplDataFormatUtil.setPriceAmountStr(mprice)
        }
      )
    } else {
      pricing
    }
  }


  /** Дефолтовое состояние Adv4Free, пробрасываемое в react+boopickle-формы,
    * поддерживающие галочку бесплатного размещения.
    *
    * @param ctx Контекст рендера.
    * @return
    */
  def a4fPropsOpt0(implicit ctx: Context): Option[MAdv4FreeProps] = {
    OptionUtil.maybe( ctx.request.user.isSuper ) {
      MAdv4FreeProps(
        fn    = AdvConstants.Su.ADV_FOR_FREE_FN,
        title = ctx.messages( MsgCodes.`Adv.for.free.without.moderation` )
      )
    }
  }
  def a4fPropsOpt0CtxFut(ctxFut: Future[Context]): Future[Option[MAdv4FreeProps]] = {
    for (ctx <- ctxFut) yield {
      a4fPropsOpt0(ctx)
    }
  }

  def a4fCheckedOpt(a4fPropsOpt: Option[MAdv4FreeProps]): Option[Boolean] = {
    a4fPropsOpt.map(_ => true)
  }



  def validateYmdIsForAdvNow(ld: LocalDate, now: LocalDate): ValidationNel[String, LocalDate] = {
    val notBeforeNow = Validation.liftNel(ld)( _.isBefore(now), "e.date.in.past" )
    val beforeFarFuture = Validation.liftNel(ld)( _.isAfter( now.plusYears(1) ), "e.date.after.far.future" )
    (notBeforeNow |@| beforeFarFuture) { (_,_) => ld }
  }


  def dateRangeYmdIsForAdv(r: MRangeYmd): ValidationNel[String, MRangeYmd] = {
    import Validation.FlatMap._

    val now = LocalDate.now()

    // Проверка одной даты, т.е. даты начала или окончания.
    def _validateOneDate(ymd: MYmd, name: String) ={
      Validation.fromTryCatchNonFatal( ymd.to[LocalDate] )
        .leftMap(_ => s"e.$name.date.invalid")
        .toValidationNel
        .flatMap( validateYmdIsForAdvNow(_, now) )
    }

    // Дата начала корректна:
    val ldStartV = _validateOneDate( r.dateStart, "start" )
    // Дата окончания корректна:
    val ldEndV = _validateOneDate( r.dateEnd, "end" )

    // Объединить оба валидатора, вернув общий кортеж.
    (ldStartV |@| ldEndV)((_,_))
      // И убедится, что дата окончания находится после даты начала.
      .flatMap { case (ldStart, ldEnd) =>
        Validation.liftNel(r)(
          {_ => !ldStart.isBefore(ldEnd) },
          "e.date.range.invalid"
        )
      }
  }


  def advPeriodInfoV(p: IPeriodInfo): ValidationNel[String, IPeriodInfo] = {
    IPeriodInfo.validateUsing(p)(dateRangeYmdIsForAdv)
  }


  def advPeriodV(advPeriod: MAdvPeriod): ValidationNel[String, MAdvPeriod] = {
    advPeriodInfoV( advPeriod.info )
      .map(_ => advPeriod)
  }

}
