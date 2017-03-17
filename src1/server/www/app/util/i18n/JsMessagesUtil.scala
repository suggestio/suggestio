package util.i18n

import com.google.inject.{Inject, Singleton}
import io.suggest.bill.MCurrencies
import io.suggest.dt.interval.{PeriodsConstants, QuickAdvPeriods}
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
    "Add" ::
      "Tags.choosing" ::
      "Add.tags" ::
      "Delete" ::
      Nil
  }

  private def DAYS_OF_WEEK_MSGS: Traversable[String] = {
    for (i <- 1 to 7) yield {
      "DayOfWeek.N." + i
    }
  }

  private def OF_MONTHS_OF_YEAR: Traversable[String] = {
    for (m <- 1 to 12) yield {
      "ofMonth.N." + m
    }
  }

  private def DATE_TIME_ABBREVATIONS: TraversableOnce[String] = {
    "year_abbrevated" ::
      Nil
  }

  /** Локализация для client-side нужд формы георазмещения. */
  private def ADV_GEO_FORM_MSGS: TraversableOnce[String] = {
    "Adv.on.map" :: "Adv.on.map.hint" ::
      "Main.screen" ::
      "GeoTag" ::
      "_adv.Online.now" ::
      "Adv.on.main.screen" ::
      "Please.wait" ::
      Nil
  }

  /** Коды ошибок форм. */
  private def FORM_ERRORS: TraversableOnce[String] = {
    "Error" ::
      "Something.gone.wrong" ::
      "error.maxLength" ::
      "error.minLength" ::
      "error.required" ::
      Nil
  }

  /** Сообщения для формы управления узлами/подузлами. */
  private def LK_NODES_MSGS: List[String] = {
    "Create" ::
      "Name" ::
      "Identifier" ::
      "Beacon.name.example" ::
      "Server.request.in.progress.wait" ::
      "Example.id.0" ::
      "Is.enabled" ::
      "Edit" ::
      "Change" ::
      "Yes" :: "No" ::
      "Are.you.sure" ::
      "Delete" ::
      "Deletion" ::
      "Subnodes" :: "N.nodes" :: "N.disabled" ::
      "Yes.delete.it" ::
      "Node.with.such.id.already.exists" ::
      "Type.new.name.for.beacon.0" ::
      "For.example.0" ::
      "Close" ::
      "Save" ::
      "Cancel" ::
      Nil
  }


  /** Коды платежных вещей в формах размещения. */
  private def ADV_PRICING: TraversableOnce[String] = {
    val prices = MCurrencies.values
      .iterator
      .map(_.i18nPriceCode)
    val msgs = {
      "Total.amount._money" ::
        "Send.request" ::
        Nil
    }
    prices ++ msgs
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
