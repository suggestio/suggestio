package util.i18n

import io.suggest.ad.blk.MBlockExpandModes

import javax.inject.{Inject, Singleton}
import io.suggest.bill.MCurrencies
import io.suggest.bill.price.dsl.MReasonTypes
import io.suggest.cal.m.MCalTypes
import io.suggest.dt.interval.QuickAdvPeriods
import io.suggest.i18n.{MLanguages, MsgCodes}
import io.suggest.mbill2.m.item.typ.MItemTypes
import jsmessages.{JsMessages, JsMessagesFactory}
import io.suggest.dt.interval.DatesIntervalConstants.{DAYS_OF_WEEK, MONTHS_OF_YEAR}
import io.suggest.ext.svc.MExtServices
import io.suggest.jd.tags.event.MJdActionTypes
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.mbill2.m.txn.MTxnTypes
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.MNodeTypes
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.payout.MEdgePayoutTypes
import io.suggest.pay.MPaySystems
import play.api.inject.Injector

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.16 10:46
  * Description: Утиль для js messages - client-side локализаций.
  */
@Singleton
final class JsMessagesUtil @Inject() (
                                       injector: Injector,
                                     ) {

  private val MC = MsgCodes

  /** Локализация для периодов рекламного размещения. */
  private def ADV_DATES_PERIOD_MSGS: IterableOnce[String] = {
    val static =
      MC.`Today` ::
      MC.`Date.choosing` ::
      MC.`Advertising.period` ::
      "Your.ad.will.adv" ::
      MC.`From._date` ::
      "from._date" ::
      MC.`till._date` ::
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
  private def TAGS_EDITOR_MSGS: IterableOnce[String] = {
    MC.`Add` ::
      MC.`Clear` ::
      MC.`Tags.choosing` ::
      MC.`Add.tags` ::
      MC.`Delete` ::
      Nil
  }

  private def DAYS_OF_WEEK_MSGS: IterableOnce[String] = {
    DAYS_OF_WEEK.iterator.flatMap { dow =>
      MC.`DayOfWeek.N.`(dow) ::
        MC.`DayOfW.N.`(dow) ::
        Nil
    }
  }

  private def OF_MONTHS_OF_YEAR: Iterable[String] = {
    for (m <- MONTHS_OF_YEAR) yield {
      MC.`ofMonth.N.`( m )
    }
  }

  private def DATE_TIME_ABBREVATIONS: List[String] = {
    "year_abbrevated" ::
      Nil
  }

  /** Локализация для client-side нужд формы георазмещения. */
  private def ADV_GEO_FORM_MSGS: IterableOnce[String] = {
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
  private def FORM_ERRORS: IterableOnce[String] = {
    MC.`Error` ::
      MC.`Something.gone.wrong` ::
      "error.maxLength" ::
      "error.minLength" ::
      "error.required" ::
      Nil
  }

  /** Сообщения для формы управления узлами/подузлами. */
  private def LK_NODES_MSGS: IterableOnce[String] = {
    val l1 = MC.`Create` ::
      MC.`Name` ::
      MC.`Identifier` ::
      MC.`Beacon.name.example` :: MC.`Wifi.router.name.example` ::
      MC.`Server.request.in.progress.wait` ::
      MC.`Example.id.0` ::
      MC.`Is.enabled` ::
      MC.`Edit` ::
      MC.`Change` ::
      MC.`Yes` :: MC.`No` ::
      MC.`Are.you.sure` ::
      MC.`Delete` ::
      MC.`Deletion` ::
      MC.`Subnodes` :: MC.`N.nodes` :: MC.`N.disabled` ::
      MC.`Yes.delete.it` ::
      MC.`New.node` ::
      "Node.with.such.id.already.exists" ::
      MC.`Type.new.name.for.beacon.0` ::
      MC.`For.example.0` ::
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
      MC.`Always.outlined` ::
      MC.`Go.into` ::
      MC.`Showcase` ::
      MC.`Distance` ::
      MC.`_to.Register._thing` ::
      MC.`Add.beacon.to.account` ::
      MC.`Parent.node` ::
      MC.`Nothing.found` ::
      MC.`Show.ad.opened` ::
      MC.`Radio.beacons` ::
      MC.`Node.type` ::
      MC.`This.action.cannot.be.undone` ::
      MC.`Write.nfc.tag` :: MC.`Make.nfc.tag.read.only` :: MC.`Bring.nfc.tag.to.device` ::
      MC.`You.can.flash.nfc.tag.with.link.to.0` :: MC.`_nfc._to.current.ad` :: MC.`_nfc._to.current.node` ::
      MC.`Also.you.can.make.nfc.tag.non.writable` ::
      Nil

    (
      l1 #::
      CAL_TYPES #::
      NODE_TYPES_I18N #::
      ADV_PRICING #::
      MNodeTypes.RadioSource.children.map(_.plural) #::
      LazyList.empty
    )
      .flatten
  }

  private def DIST_UNITS: IterableOnce[String] = {
    MC.`n.km._kilometers` ::
    MC.`n.m._meters` ::
    MC.`n.cm._centimeters` ::
    Nil
  }

  /** Коды платежных вещей в формах размещения. */
  private def ADV_PRICING: IterableOnce[String] = {
    val prices = MCurrencies.values
      .iterator
      .flatMap { c =>
        c.i18nPriceCode :: c.currencyNameI18n :: Nil
      }
    val msgs = {
      MC.`Total.amount._money` ::
      MC.`Send.request` ::
      Nil
    }
    prices ++ msgs
  }


  /** пн, вт, ср, ... */
  private def DOWS_LC: IterableOnce[String] = {
    DAYS_OF_WEEK.map { MC.`dayOfW.N.` }
  }

  /** Названия календарей. */
  private def CAL_TYPES: IterableOnce[String] = {
    MCalTypes.values
      .iterator
      .map(_.i18nCode)
  }

  /** Коды сообщений инфы по размещению. */
  private def ADV_INFO: IterableOnce[String] = {
    val msgs = MC.`Please.try.again.later` ::
      MC.`Tariff.rate.of.0` ::
      MC.`Information` ::
      MC.`day24h` ::
      MC.`Minimal.module` ::
      MC.`Adv.on.main.screen` :: MC.`Adv.in.tag` ::
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

  private def LK_COMMON: IterableOnce[String] = {
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

  private def LK_ADN_MAP_MSGS: IterableOnce[String] = {
    MC.`You.can.place.adn.node.on.map.below` ::
      MC.`Your.sc.will.be.opened.auto.when.user.geolocated.inside.circle` ::
      MReasonTypes.GeoLocCapture.msgCodeI18n ::
      Nil
  }


  /** Сообщения для react-выдачи. */
  private def SC: IterableOnce[String] = {
    val SC_ONLY_MSGS = MC.`Map` ::
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
      // wizard: Continue/later - from https://developer.apple.com/design/human-interface-guidelines/ios/app-architecture/accessing-user-data/
      MC.`Continue` :: MC.`Later` ::
      MC.`You.can.enable.0.later.on.left.panel` ::
      MC.`Next` ::
      MC.`Settings.done.0.ready.for.using` ::
      MC.`Try.again` ::
      MC.`Close` ::
      ErrorMsgs.GET_NODE_INDEX_FAILED ::
      ErrorMsgs.XHR_UNEXPECTED_RESP ::
      MC.`Application` ::
      MC.`Download.application` ::
      MC.`Download.0` ::
      MC.`No.downloads.available` ::
      MC.`File` ::
      MC.`Malfunction` ::
      MC.`Choose...` ::
      MC.`Open.0` ::
      MC.`Size` ::
      MC.`Install` ::
      MC.`Type` ::
      MC.`Information` ::
      MC.`Show` :: MC.`Hide` ::
      MC.`Settings` ::
      MC.`0.not.supported.on.this.platform.1` :: MC.`Install.app.for.access.to.0` ::
      MC.`Browser` ::
      MC.`Notifications` ::
      MC.`Notify.about.offers.nearby` ::
      MC.`0.offers.nearby` :: MC.`One.offer.nearby` ::
      MC.`0._inDistance.1` ::
      MC.`in.radius.of.0.from.1` ::
      MC.`Show.offers.0` ::
      MC.`in.radius.of.0` ::
      MC.`Background.mode` ::
      MC.`Background.monitoring.offers.nearby` ::
      MC.`Daemon.toast.title` ::
      MC.`Daemon.toast.descr` ::
      MC.`Unsupported.browser.or.fatal.failure` ::
      MC.`Not.connected` :: MC.`Retry` ::
      MC.`Recents` ::
      MC.`Nodes.management` ::
      MC.`To.control.beacons.need.login` ::
      MC.`Mode` ::
      MC.`Ad.adv.manage` ::
      MC.`Bg.location` :: MC.`Bg.location.hint` ::
      ErrorMsgs.SC_FSM_EVENT_FAILED ::
      MC.`Language` :: MC.`System._adjective` ::
      MC.`Requesting.permission` ::
      MC.`Current.location` ::
      MC.`About.sio.node.id` ::
      MLanguages.values.iterator.map(_.singularMsgCode).toList

    (
      SC_ONLY_MSGS #::
      LOGIN_FORM_MSGS #::
      LK_NODES_MSGS #::
      LazyList.empty
    )
      .flatten
  }


  /** Сообщения для react-редактора карточек. */
  private def LK_AD_EDIT_MSGS: IterableOnce[String] = {
    val codes1 = MC.`Width` ::
      MC.`Height` ::
      MC.`Delete.block` ::
      MC.`Description` ::
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
      MC.`Line.height` ::
      MC.`Title` ::
      MC.`Outline` :: MC.`Transparent` :: MC.`Define.manually` ::
      MC.`Events` ::
      MC.`Choose...` ::
      MC.`Action` ::
      Nil

    (
      codes1 ::
      MBlockExpandModes.values.iterator.map(_.msgCode) ::
      MJdActionTypes.values.iterator.map(_.i18nCode) ::
      Nil
    )
      .iterator
      .flatten
  }


  private def LK_ADS_MSGS: IterableOnce[String] = {
    MC.`Ad.cards` ::
      MC.`You.can.look.on.ads.in` ::
      MC.`_ad.showcase.link` ::
      MC.`Create.ad` ::
      MC.`Show._ad` ::
      MC.`ad.Manage` ::
      Nil
  }

  private def LK_ADN_EDIT_MSGS: IterableOnce[String] = {
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

  private def TXN_TYPES_I18N: List[String] = {
    MTxnTypes.values
      .iterator
      .map(_.i18nCode)
      .toList
  }

  private def NODE_TYPES_I18N: List[String] = {
    MNodeTypes.values
      .iterator
      .map(_.singular)
      .toList
  }

  /** Клиент-сайд корзина/биллинг. */
  private def BILL_CART_MSGS: IterableOnce[String] = {
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
      MC.`Transactions` ::
      MC.`Cancel.order...` ::
      MC.`Check.your.internet.connection.and.retry` ::
      ErrorMsgs.CANNOT_CONNECT_TO_REMOTE_SYSTEM ::
      (ORDER_STATUSES_I18N reverse_:::
       ITEM_STATUSES_I18N reverse_:::
       NODE_TYPES_I18N reverse_:::
       TXN_TYPES_I18N
      )
  }

  private def SYS_MDR_MSGS: IterableOnce[String] = {
    MC.`All` ::
      MPredicates.Receiver.Self.singular ::
      MC.`Something.gone.wrong` ::
      MC.`Nothing.to.moderate` ::
      MC.`No.incoming.adv.requests` ::
      MC.`Lost.node` ::
      MC.`Approve` ::
      MC.`Reload` ::
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
      MC.`till._date` ::
      MC.`in.radius.of.0.from.1` ::
      MC.`From._date` ::
      MC.`Node` ::
      MC.`Edit` ::
      (ITEM_TYPES reverse_:::
        DATE_TIME_ABBREVATIONS reverse_:::
        OF_MONTHS_OF_YEAR.toList
      )
  }


  private def LOGIN_FORM_MSGS: List[String] = {
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
    MC.`Logout.account` :: MC.`Logout` ::
    MC.`Are.you.sure.to.logout.account` ::
    MC.`Input.text.from.picture` ::
    MC.`_accept.privacy.policy` ::
    Nil
  }


  private def EDGE_EDIT_FORM_MSGS: IterableOnce[String] = {
    // Одиночные строки перечиляются здесь:
    val heads =
      MC.`Predicate` ::
      MC.`Node.ids.or.keys` ::
      MC.`Add` ::
      MC.`Flag.value` ::
      MC.`Ordering` ::
      MC.`Not.indexed.text` ::
      MC.`Type.text...` ::
      MC.`Delete` ::
      MC.`Are.you.sure` ::
      MC.`Deletion` ::
      MC.`Save` ::
      MC.`Size` ::
      MC.`Original` ::
      MC.`Media` ::
      MC.`Mime.type` ::
      MC.`File` ::
      MC.`Storage` ::
      MC.`Type` ::
      MC.`Metadata` ::
      MC.yesNo(true) :: MsgCodes.yesNo(false) ::
      MC.`Open.file` ::
      MC.`Go.to.node.0` ::
      MC.`Replace.node.ids` ::
      MC.`Append.to.node.ids` ::
      MC.`Cancel` ::
      MC.`File.already.exist.on.node.0` ::
      MC.`External.service` ::
      MC.`empty` ::
      MC.`Info` ::
      MC.`Operating.system.family` ::
      MC.`Pay.system` :: MC.`Payout` ::
      Nil

    // Оптовые списки сообщений - тут:
    ( heads #::
      MPredicates.values.iterator.map(_.singular) #::
      MExtServices.values.iterator.map(_.nameI18N) #::
      MPaySystems.values.iterator.map(_.nameI18n) #::
      MEdgePayoutTypes.values.iterator.map(_.singularI18n) #::
      LazyList.empty
    )
      .iterator
      .flatten
  }


  /** Короткие (сокращённых) суффиксы для типичных префиксов. */
  private def KMGT_UNITS: List[String] = {
    MC.`K._Kilo` ::
      MC.`M._Mega` ::
      MC.`G._Giga` ::
      MC.`T._Tera` ::
      MC.`Number.frac.delim` ::
      MC.`Number0.with.units1` ::
      MC.`Prefixed0.metric.unit1` ::
      MC.`B._Bytes` ::    // Тут байты, обычно если что-то измеряется, то в байтах.
      Nil
  }


  /** Контейнер информации для js-сообщений. */
  case class JsMsgsInfo(
                         jsMessages     : JsMessages,
                         hash           : Int,
                       )

  val (lk, sys, sc) = {
    val jsMessagesFactory = injector.instanceOf[JsMessagesFactory]

    def _mkMsgsInfo(msgs: Set[String]): JsMsgsInfo = {
      // НЕ используем .subset() т.к. он медленнный O(N), но код и результат эквивалентен .filtering( coll.contains )
      // При использовании Set[String] сложность будет O(ln2 N), + дедубликация одинаковых ключей.
      val jsm = jsMessagesFactory.filtering( msgs.contains )
      val hash = msgs.hashCode()
      JsMsgsInfo(jsm, hash)
    }


    /** Готовенькие сообщения для раздачи через js сообщения на всех поддерживаемых языках. */
    val _lk = _mkMsgsInfo(
      (
        ADV_DATES_PERIOD_MSGS #::
        TAGS_EDITOR_MSGS #::
        ADV_GEO_FORM_MSGS #::
        FORM_ERRORS #::
        OF_MONTHS_OF_YEAR #::
        DAYS_OF_WEEK_MSGS #::
        DATE_TIME_ABBREVATIONS #::
        ADV_PRICING #::
        ADV_INFO #::
        DIST_UNITS #::
        ITEM_TYPES #::
        LK_ADN_MAP_MSGS #::
        LK_COMMON #::
        LK_NODES_MSGS #::
        LK_AD_EDIT_MSGS #::
        LK_ADS_MSGS #::
        LK_ADN_EDIT_MSGS #::
        BILL_CART_MSGS #::
        SYS_MDR_MSGS #::
        LOGIN_FORM_MSGS #::
        LazyList.empty
      )
        .iterator
        .flatten
        .toSet
    )


    /** Сообщения для системной панели. */
    val _sys = _mkMsgsInfo(
      (
        SYS_MDR_MSGS #::
        EDGE_EDIT_FORM_MSGS #::
        LazyList.empty
      )
        .iterator
        .flatten
        .toSet
    )



    /** jsMessages для выдачи. */
    val _sc: JsMessages = {
      val msgs = (
        SC #::
        DIST_UNITS #::
        KMGT_UNITS #::
        LazyList.empty
      )
        .iterator
        .flatten
        .toSet

      val jsm = jsMessagesFactory.filtering( msgs.contains )
      // TODO Вычислять hash для кеширования?
      jsm
    }

    ( _lk, _sys, _sc )
  }

}
