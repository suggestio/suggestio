package util.i18n

import javax.inject.{Inject, Singleton}
import io.suggest.bill.MCurrencies
import io.suggest.bill.price.dsl.MReasonTypes
import io.suggest.cal.m.MCalTypes
import io.suggest.dt.interval.{PeriodsConstants, QuickAdvPeriods}
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.item.typ.MItemTypes
import jsmessages.{JsMessages, JsMessagesFactory}
import io.suggest.dt.interval.DatesIntervalConstants.{MONTHS_OF_YEAR, DAYS_OF_WEEK}

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

  private val MC = MsgCodes

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
    MC.`Add` ::
      "Tags.choosing" ::
      MC.`Add.tags` ::
      MC.`Delete` ::
      Nil
  }

  private def DAYS_OF_WEEK_MSGS: TraversableOnce[String] = {
    DAYS_OF_WEEK.iterator.flatMap { dow =>
      MC.`DayOfWeek.N.`(dow) ::
        MC.`DayOfW.N.`(dow) ::
        Nil
    }
  }

  private def OF_MONTHS_OF_YEAR: Traversable[String] = {
    for (m <- MONTHS_OF_YEAR) yield {
      MC.`ofMonth.N.`( m )
    }
  }

  private def DATE_TIME_ABBREVATIONS: TraversableOnce[String] = {
    "year_abbrevated" ::
      Nil
  }

  /** Локализация для client-side нужд формы георазмещения. */
  private def ADV_GEO_FORM_MSGS: TraversableOnce[String] = {
    MC.`Adv.on.map` :: MC.`Adv.on.map.hint` ::
      MC.`Main.screen` ::
      MC.`GeoTag` ::
      MC.`_adv.Online.now` ::
      MC.`Radius` :: MC.`in.radius.of.0.from.1` ::
      MC.`N.modules` ::
      MC.`Adv.on.main.screen` ::
      MC.`Unable.to.initialize.map` ::
      MC.`Please.wait` ::
      MC.`Adv.on.main.screen` :: MC.`Coverage.area` ::
      MC.`Ad.area.modules.count` :: MC.`Tag` ::
      MC.`Date` :: MC.`Price` ::
      // Как бы документация.
      MC.`Read.more` ::
      MC.`Adv.geo.form.descr1` ::
      MC.`User.located.inside.are.or.location.will.see.your.offer` ::
      MC.`Good.to.know.that` ::
      MC.`Geo.circle.does.not.touch.any.nodes` ::
      MC.`Nodes.can.contain.subnodes.sublocations.routers.tvs.beacons` ::
      MC.`Adv.geo.form.descr.price` ::
      Nil
  }

  /** Коды ошибок форм. */
  private def FORM_ERRORS: TraversableOnce[String] = {
    MC.`Error` ::
      "Something.gone.wrong" ::
      "error.maxLength" ::
      "error.minLength" ::
      "error.required" ::
      Nil
  }

  /** Сообщения для формы управления узлами/подузлами. */
  private def LK_NODES_MSGS: TraversableOnce[String] = {
    val l1 = MC.`Create` ::
      MC.`Name` ::
      "Identifier" ::
      "Beacon.name.example" ::
      "Server.request.in.progress.wait" ::
      "Example.id.0" ::
      MC.`Is.enabled` ::
      MC.`Edit` ::
      MC.`Change` ::
      MC.`Yes` :: MC.`No` ::
      "Are.you.sure" ::
      MC.`Delete` ::
      MC.`Deletion` ::
      MC.`Subnodes` :: MC.`N.nodes` :: MC.`N.disabled` ::
      MC.`Yes.delete.it` ::
      "New.node" ::
      "Node.with.such.id.already.exists" ::
      MC.`Type.new.name.for.beacon.0` ::
      "For.example.0" ::
      MC.`Show.details` ::
      MC.`Close` ::
      MC.`Save` ::
      MC.`Cancel` ::
      // Тарифы узлов
      MC.`Adv.tariff` ::
      MC.`Inherited` :: MC.`Set.manually` ::
      MC.`_per_.day` ::
      MC.`Comission.0.pct.for.sio` ::
      MC.`Cost` ::
      Nil

    Iterator(l1, CAL_TYPES)
      .flatten
  }

  private def DIST_UNITS: TraversableOnce[String] = {
    MC.`n.km._kilometers` ::
      MC.`n.m._meters` ::
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


  /** пн, вт, ср, ... */
  private def DOWS_LC: TraversableOnce[String] = {
    DAYS_OF_WEEK.map { MC.`dayOfW.N.` }
  }

  /** Названия календарей. */
  private def CAL_TYPES: TraversableOnce[String] = {
    MCalTypes.values
      .iterator
      .map(_.name)
  }

  /** Коды сообщений инфы по размещению. */
  private def ADV_INFO: TraversableOnce[String] = {
    val msgs = MC.`Please.try.again.later` ::
      MC.`Tariff.rate.of.0` ::
      MC.`Information` ::
      MC.`day24h` ::
      MC.`Minimal.module` ::
      MC.`scheme.left` ::
      MC.`Current.ad` ::
      MC.`N.modules` ::
      MC.`Agreement.btw.CBCA.and.node.tariffs.for.year` ::
      MC.`Town` ::
      MC.`Address` ::
      MC.`Site` ::
      MC.`Info.about.prods.and.svcs` ::
      MC.`Daily.people.traffic` ::
      MC.`Audience.descr` ::
      Nil

    Iterator( msgs, DOWS_LC, CAL_TYPES )
      .flatten
  }

  private def LK_COMMON: TraversableOnce[String] = {
    MC.`Something.gone.wrong` ::
      Nil
  }

  private def ITEM_TYPES: TraversableOnce[String] = {
    for (i <- MItemTypes.values.iterator) yield {
      i.nameI18n
    }
  }

  private def LK_ADN_MAP_MSGS: TraversableOnce[String] = {
    MC.`You.can.place.adn.node.on.map.below` ::
      MC.`Your.sc.will.be.opened.auto.when.user.geolocated.inside.circle` ::
      MReasonTypes.GeoLocCapture.msgCodeI18n ::
      Nil
  }


  /** Сообщения для react-выдачи. */
  private def SC: TraversableOnce[String] = {
    MC.`Quick.search.for.offers` ::
      MC.`Map` ::
      MC.`Tags` ::
      MC.`No.tags.here` ::
      MC.`Login.page.title` ::
      MC.`Go.to.node.ads` ::
      MC.`Personal.cabinet` ::
      MC.`Suggest.io.Project` ::
      MC.`Suggest.io._transcription` ::
      MC.`Edit` ::
      Nil
  }


  /** Сообщения для react-редактора карточек. */
  private def LK_AD_EDIT_MSGS: TraversableOnce[String] = {
    MC.`Width` ::
      MC.`Height` ::
      MC.`Delete.block` ::
      MC.`What.to.add` ::
      MC.`Block` ::
      MC.`Content` ::
      MC.`Example.text` ::
      MC.`Bg.color` ::
      MC.`File.is.not.a.picture` ::
      MC.`Apply` ::
      MC.`Crop` ::
      MC.`Picture.editing` ::
      MC.`Cannot.checksum.file` ::
      MC.`Suggested.bg.colors` ::
      MC.`Scale` ::
      MC.`Preparing` ::
      MC.`Uploading.file` ::
      MC.`Saving` ::
      MC.`Saved` ::
      MC.`Stretch.across` ::
      MC.`Show` ::
      MC.`Main.block` ::
      MC.`Main.blocks.are.used.to.display.ad.inside.the.grid` ::
      MC.`This.block.is.the.only.main` ::
      MC.`This.block.is.not.main` ::
      MC.`No.main.blocks.First.block.is.main` ::
      MC.`Background` ::
      MC.`Upload.file` ::
      MC.`Rotation` ::
      Nil
  }


  private def LK_ADS_MSGS: TraversableOnce[String] = {
    MC.`Ad.cards` ::
      MC.`You.can.look.on.ads.in` ::
      MC.`_ad.showcase.link` ::
      MC.`Create.ad` ::
      MC.`Show._ad` ::
      MC.`ad.Manage` ::
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
      LK_NODES_MSGS,
      LK_AD_EDIT_MSGS,
      LK_ADS_MSGS
    )
      .flatten
      .toSet

    // НЕ используем .subset() т.к. он медленнный O(N), но код и результат эквивалентен .filtering( coll.contains )
    // При использовании Set[String] сложность будет O(ln2 N), + дедубликация одинаковых ключей.
    val jsm = jsMessagesFactory.filtering( msgs.contains )
    val hash = msgs.hashCode()
    (jsm, hash)
  }


  /** jsMessages для выдачи. */
  val scJsMsgsFactory: JsMessages = {
    val msgs = SC.toSet
    val jsm = jsMessagesFactory.filtering( msgs.contains )
    // TODO Вычислять hash для кеширования?
    jsm
  }

}

/** Интерфейс для DI-поля с инстансом [[JsMessagesUtil]]. */
trait IJsMessagesUtilDi {
  val jsMessagesUtil: JsMessagesUtil
}
