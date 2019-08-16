package util.i18n

import io.suggest.ad.blk.MBlockExpandModes
import javax.inject.{Inject, Singleton}
import io.suggest.bill.MCurrencies
import io.suggest.bill.price.dsl.MReasonTypes
import io.suggest.cal.m.MCalTypes
import io.suggest.dt.interval.QuickAdvPeriods
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.item.typ.MItemTypes
import jsmessages.{JsMessages, JsMessagesFactory}
import io.suggest.dt.interval.DatesIntervalConstants.{DAYS_OF_WEEK, MONTHS_OF_YEAR}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.MNodeTypes

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

    val advPeriodsIter = QuickAdvPeriods
      .values
      .iterator
      .map(_.messagesCode)

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
      .map(_.i18nCode)
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
    MC.`Input.text.from.picture`  ::
    Nil
  }

  private def ITEM_TYPES: List[String] = {
    MItemTypes
      .values
      .iterator
      .map(_.nameI18n)
      .toList
  }

  private def LK_ADN_MAP_MSGS: TraversableOnce[String] = {
    MC.`You.can.place.adn.node.on.map.below` ::
      MC.`Your.sc.will.be.opened.auto.when.user.geolocated.inside.circle` ::
      MReasonTypes.GeoLocCapture.msgCodeI18n ::
      Nil
  }


  /** Сообщения для react-выдачи. */
  private def SC: TraversableOnce[String] = {
      MC.`Map` ::
      MC.`Tags` ::
      MC.`No.tags.here` ::
      MC.`No.tags.found.for.1.query` ::
      MC.`Login.page.title` ::
      MC.`Go.to.node.ads` ::
      MC.`Personal.cabinet` ::
      MC.`Suggest.io.Project` ::
      MC.`Suggest.io._transcription` ::
      MC.`Edit` ::
      MC.`Please.wait` ::
      MC.`On` ::
      MC.`Off` ::
      MC.`Clear` ::
      MC.`Something.gone.wrong` ::
      MC.`Error` ::
      MC.`Search.start.typing` ::
      MC.`Geolocation` ::
      MC.`Go.into` ::
      MC.`Location.changed` ::
      MC.`Cancel` ::
      MC.`Go.back` ::
      MC.`Go.back.to.0` ::
      // wizard:
      MC.`0.uses.geoloc.to.find.ads` ::
      MC.`0.uses.bt.to.find.ads.indoor` ::
      MC.`Allow.0` ::
      MC.`Later` ::
      MC.`You.can.enable.0.later.on.left.panel` ::
      MC.`Next` ::
      MC.`Settings.done.0.ready.for.using` ::
      MC.`_to.Finish` ::
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
      MC.`Layer` ::
      MC.`Layers` ::
      MC.`Uppermost` ::
      MC.`Downmost` ::
      MC.`Above` ::
      MC.`Below` ::
      MC.`Text.shadow` ::
      MC.`Color` ::
      MC.`Shadow.color` ::
      MC.`Expand` ::
      MC.`Dont.expand` ::
      MBlockExpandModes.values.iterator.map(_.msgCode).toList
  }


  private def LK_ADS_MSGS: TraversableOnce[String] = {
    MC.`Ad.cards` ::
      MC.`You.can.look.on.ads.in` ::
      MC.`_ad.showcase.link` ::
      MC.`Create.ad` ::
      MC.`Show._ad` ::
      MC.`ad.Manage` ::
      MC.`Show.ad.opened` ::
      Nil
  }

  private def LK_ADN_EDIT_MSGS: TraversableOnce[String] = {
    MC.`Name` ::
      MC.`Town` ::
      MC.`Address` ::
      MC.`Site` ::
      MC.`Info.about.prods.and.svcs` ::
      MC.`Daily.people.traffic` ::
      MC.`Audience.descr` ::
      MC.`Bg.color` ::
      MC.`Fg.color.of.sc.hint` ::
      MC.`Welcome.screen` ::
      MC.`Welcome.bg.hint` ::
      MC.`Node.photos` ::
      MC.`Save` ::
      Nil
  }


  private def ORDER_STATUSES_I18N: List[String] = {
    MOrderStatuses.values
      .iterator
      .map(_.singular)
      .toList
  }

  private def ITEM_STATUSES_I18N: List[String] = {
    MItemStatuses.values
      .iterator
      .map(_.nameI18n)
      .toList
  }

  private def NODE_TYPES_I18N: List[String] = {
    MNodeTypes.values
      .iterator
      .map(_.singular)
      .toList
  }

  /** Клиент-сайд корзина/биллинг. */
  private def BILL_CART_MSGS: TraversableOnce[String] = {
    MC.`_order.Items` ::
      MC.`Price` ::
      MC.`Delete` ::
      MC.`Tag` ::
      MC.`Node` ::
      MC.`Adv.on.map` ::
      MC.`in.radius.of.0.from.1` ::
      MC.`Your.cart.is.empty` ::
      MC.`N.selected` ::
      MC.`Total` ::
      MC.`Cart` ::
      MC.`Reload` ::
      MC.`Pay` ::
      MC.`Order.N` ::
      MC.`Bill.id` ::
      MC.`Bill.details` ::
      MC.`Date` ::
      MC.`Sum` ::
      MC.`No.transactions.found` ::
      MC.`Payment.for.order.N` ::
      MC.`Transactions` ::
      (ORDER_STATUSES_I18N reverse_::: ITEM_STATUSES_I18N reverse_::: NODE_TYPES_I18N)
  }

  private def SYS_MDR_MSGS: TraversableOnce[String] = {
    MC.`All` ::
      MPredicates.Receiver.Self.singular ::
      MC.`Something.gone.wrong` ::
      MC.`Nothing.to.moderate` ::
      MC.`No.incoming.adv.requests` ::
      MC.`Lost.node` ::
      MC.`Approve` ::
      MC.`Refuse` ::
      MC.`Reason` ::
      MC.`Cancel` ::
      MC.`Error` ::
      MC.`Previous.node` ::
      MC.`Next.node` ::
      MC.`To.beginning` ::
      MC.`To.end` ::
      MC.`Moderate.all.nodes` ::
      MC.`Moderate.requests.from.all.nodes` ::
      MC.`_N` ::
      ITEM_TYPES
  }


  private def LOGIN_FORM_MSGS: TraversableOnce[String] = {
    MC.`Login.page.title` ::
    MC.`Login` ::
    MC.`Username` ::
    MC.`Password` ::
    MC.`Not.my.pc` ::
    MC.`ESIA._unabbrevated` ::
    MC.`I.accept` ::
    MC.`terms.of.service` ::
    MC.`I.allow.personal.data.processing` ::
    MC.`_to.Finish` ::
    MC.`Your.email.addr` ::
    MC.`Sign.up` ::
    MC.`Email.example` ::
    MC.`Phone.number.example` ::
    MC.`Mobile.phone.number` ::
    MC.`Phone.or.email` ::
    MC.`Code.from.sms` ::
    MC.`Next` ::
    MC.`Back` ::
    MC.`Sms.code.sent.to.your.phone.number.0` ::
    MC.`Type.password` ::
    MC.`Retype.password` ::
    MC.`Type.current.password` ::
    MC.`Type.new.password` ::
    MC.`Confirm.new.password` ::
    MC.`Type.previous.signup.data.to.start.password.recovery` ::
    MC.`Forgot.password` ::
    MC.`Password.change` ::
    MC.`New.password.saved` ::
    MC.`Inacceptable.password.format` ::
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
      LK_ADS_MSGS,
      LK_ADN_EDIT_MSGS,
      BILL_CART_MSGS,
      SYS_MDR_MSGS,
      LOGIN_FORM_MSGS,
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
    val msgs = Iterator(
      SC,
      DIST_UNITS
    )
      .flatten
      .toSet
    val jsm = jsMessagesFactory.filtering( msgs.contains )
    // TODO Вычислять hash для кеширования?
    jsm
  }

}

/** Интерфейс для DI-поля с инстансом [[JsMessagesUtil]]. */
trait IJsMessagesUtilDi {
  val jsMessagesUtil: JsMessagesUtil
}
