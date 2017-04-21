package util.i18n

import com.google.inject.{Inject, Singleton}
import io.suggest.bill.MCurrencies
import io.suggest.bill.price.dsl.MReasonTypes
import io.suggest.cal.m.MCalTypes
import io.suggest.dt.interval.{PeriodsConstants, QuickAdvPeriods}
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.item.typ.MItemTypes
import jsmessages.{JsMessages, JsMessagesFactory}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.16 10:46
  * Description: Утиль для js messages - client-side локализаций.
  */
@Singleton
class JsMessagesUtil @Inject() (
  jsMessagesFactory     : JsMessagesFactory
) {

  /** Локализация для периодов рекламного размещения. */
  private def ADV_DATES_PERIOD_MSGS: TraversableOnce[String] = {
    val static = "Today" ::
      "Date.choosing" ::
      "Advertising.period" ::
      "Your.ad.will.adv" ::
      "From._date" ::
      "from._date" ::
      "till._date" ::
      "Date.start" ::
      "Date.end" ::
      "locale.momentjs" ::
      Nil

    val advPeriodsIter: Iterator[String] = {
      Seq(
        QuickAdvPeriods.values
          .iterator
          .map(_.messagesCode),
        Seq( PeriodsConstants.MESSAGES_PREFIX + PeriodsConstants.CUSTOM )
      )
        .iterator
        .flatten
    }

    Iterator(static, advPeriodsIter)
      .flatten
  }


  /** Сообщения редактора тегов. */
  private def TAGS_EDITOR_MSGS: TraversableOnce[String] = {
    MsgCodes.`Add` ::
      "Tags.choosing" ::
      MsgCodes.`Add.tags` ::
      MsgCodes.`Delete` ::
      Nil
  }

  private def DAYS_OF_WEEK_MSGS: TraversableOnce[String] = {
    (1 to 7).iterator.flatMap { dow =>
      MsgCodes.`DayOfWeek.N.`(dow) ::
        MsgCodes.`DayOfW.N.`(dow) ::
        Nil
    }
  }

  private def OF_MONTHS_OF_YEAR: Traversable[String] = {
    for (m <- 1 to 12) yield {
      MsgCodes.`ofMonth.N.`( m )
    }
  }

  private def DATE_TIME_ABBREVATIONS: TraversableOnce[String] = {
    "year_abbrevated" ::
      Nil
  }

  /** Локализация для client-side нужд формы георазмещения. */
  private def ADV_GEO_FORM_MSGS: TraversableOnce[String] = {
    MsgCodes.`Adv.on.map` :: MsgCodes.`Adv.on.map.hint` ::
      "Main.screen" ::
      "GeoTag" ::
      "_adv.Online.now" ::
      MsgCodes.`Radius` :: MsgCodes.`in.radius.of.0.from.1` ::
      MsgCodes.`N.modules` ::
      "Adv.on.main.screen" ::
      MsgCodes.`Unable.to.initialize.map` ::
      MsgCodes.`Please.wait` ::
      MsgCodes.`Adv.on.main.screen` :: MsgCodes.`Coverage.area` ::
      MsgCodes.`Ad.area.modules.count` :: MsgCodes.`Tag` ::
      MsgCodes.`Date` :: MsgCodes.`Price` ::
      // Как бы документация.
      MsgCodes.`Read.more` ::
      MsgCodes.`Adv.geo.form.descr1` ::
      MsgCodes.`User.located.inside.are.or.location.will.see.your.offer` ::
      MsgCodes.`Good.to.know.that` ::
      MsgCodes.`Geo.circle.does.not.touch.any.nodes` ::
      MsgCodes.`Nodes.can.contain.subnodes.sublocations.routers.tvs.beacons` ::
      MsgCodes.`Adv.geo.form.descr.price` ::
      Nil
  }

  /** Коды ошибок форм. */
  private def FORM_ERRORS: TraversableOnce[String] = {
    MsgCodes.`Error` ::
      "Something.gone.wrong" ::
      "error.maxLength" ::
      "error.minLength" ::
      "error.required" ::
      Nil
  }

  /** Сообщения для формы управления узлами/подузлами. */
  private def LK_NODES_MSGS: TraversableOnce[String] = {
    val l1 = MsgCodes.`Create` ::
      MsgCodes.`Name` ::
      "Identifier" ::
      "Beacon.name.example" ::
      "Server.request.in.progress.wait" ::
      "Example.id.0" ::
      MsgCodes.`Is.enabled` ::
      MsgCodes.`Edit` ::
      MsgCodes.`Change` ::
      MsgCodes.`Yes` :: MsgCodes.`No` ::
      "Are.you.sure" ::
      MsgCodes.`Delete` ::
      MsgCodes.`Deletion` ::
      MsgCodes.`Subnodes` :: MsgCodes.`N.nodes` :: MsgCodes.`N.disabled` ::
      MsgCodes.`Yes.delete.it` ::
      "New.node" ::
      "Node.with.such.id.already.exists" ::
      MsgCodes.`Type.new.name.for.beacon.0` ::
      "For.example.0" ::
      MsgCodes.`Show.details` ::
      MsgCodes.`Close` ::
      MsgCodes.`Save` ::
      MsgCodes.`Cancel` ::
      // Тарифы узлов
      MsgCodes.`Adv.tariff` ::
      MsgCodes.`Inherited` :: MsgCodes.`Set.manually` ::
      MsgCodes.`_per_.day` ::
      MsgCodes.`Comission.0.pct.for.sio` ::
      MsgCodes.`Cost` ::
      Nil

    val l2 = MCalTypes.values.map(_.name)

    Iterator(l1, l2).flatten
  }

  private def DIST_UNITS: TraversableOnce[String] = {
    MsgCodes.`n.km._kilometers` ::
      MsgCodes.`n.m._meters` ::
      Nil
  }

  /** Коды платежных вещей в формах размещения. */
  private def ADV_PRICING: TraversableOnce[String] = {
    val prices = MCurrencies.values
      .iterator
      .flatMap { c =>
        c.i18nPriceCode :: c.currencyNameI18n :: Nil
      }
    val msgs = {
      "Total.amount._money" ::
        "Send.request" ::
        Nil
    }
    prices ++ msgs
  }


  /** Коды сообщений инфы по размещению. */
  private def ADV_INFO: TraversableOnce[String] = {
    MsgCodes.`Information` ::
      Nil
  }

  private def LK_COMMON: TraversableOnce[String] = {
    MsgCodes.`Something.gone.wrong` ::
      Nil
  }

  private def ITEM_TYPES: TraversableOnce[String] = {
    for (i <- MItemTypes.values.iterator) yield {
      i.nameI18n
    }
  }

  private def LK_ADN_MAP_MSGS: TraversableOnce[String] = {
    MsgCodes.`You.can.place.adn.node.on.map.below` ::
      MReasonTypes.AdnMapAdv.msgCodeI18n ::
      Nil
  }

  /** Готовенькие сообщения для раздачи через js сообщения на всех поддерживаемых языках. */
  val (lkJsMsgsFactory, hash): (JsMessages, Int) = {
    val msgs = Iterator(
      ADV_DATES_PERIOD_MSGS,
      TAGS_EDITOR_MSGS,
      ADV_GEO_FORM_MSGS,
      FORM_ERRORS,
      OF_MONTHS_OF_YEAR,
      DAYS_OF_WEEK_MSGS,
      DATE_TIME_ABBREVATIONS,
      ADV_PRICING,
      ADV_INFO,
      DIST_UNITS,
      ITEM_TYPES,
      LK_ADN_MAP_MSGS,
      LK_COMMON,
      LK_NODES_MSGS
    )
      .flatten
      .toSet

    // НЕ используем .subset() т.к. он медленнный O(N), но код и результат эквивалентен .filtering( coll.contains )
    // При использовании Set[String] сложность будет O(ln2 N), + дедубликация одинаковых ключей.
    val jsm = jsMessagesFactory.filtering( msgs.contains )
    val hash = msgs.hashCode()
    (jsm, hash)
  }

}

/** Интерфейс для DI-поля с инстансом [[JsMessagesUtil]]. */
trait IJsMessagesUtilDi {
  val jsMessagesUtil: JsMessagesUtil
}
