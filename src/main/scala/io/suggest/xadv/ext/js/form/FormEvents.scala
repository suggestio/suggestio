package io.suggest.xadv.ext.js.form

import io.suggest.sjs.common.controller.RoutedInitController
import org.scalajs.dom
import org.scalajs.dom.{Element, XMLHttpRequest}
import org.scalajs.jquery._
import io.suggest.adv.ext.view.FormPage._

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.{JSApp, Any}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.03.15 15:09
 * Description: Подписка и обработка событий формы размещения.
 */

/** Реализация контроллера с запуска экшеном, который связанн с формой. */
trait FormEventsRiCtl extends RoutedInitController with FormEventsT {
  /**
   * Запустить на исполнение экшен.
   * @param name Название экшена, заданное в body.
   * @return Фьючерс с результатом.
   */
  override def riAction(name: String): Future[_] = {
    if (name startsWith "forAd") {
      Future {
        bindFormEvents()
      }
    } else {
      super.riAction(name)
    }
  }
}


object FormEvents extends JSApp with FormEventsT {

  /** Запуск скрипта на исполнение по требованию извне. */
  @JSExport
  override def main(): Unit = {
    bindFormEvents()
  }

}


trait FormEventsT {
  /** Забиндить все события формы, вдохнув в неё жизнь. */
  protected def bindFormEvents(): Unit = {
    bindAddTargetClick()
    bindDeleteTargetClick()
    bindOneTgChange()
    bindOneTgSubmit()
    bindAdvFormSubmit()
    bindReturnCheckboxRadio()
  }


  /** Забиндиться на событие клика по кнопке добавления новой цели. */
  private def bindAddTargetClick(): Unit = {
    // Обработчик события клика по добавлению таргета
    val listener = { evt: JQueryEventObject =>
      evt.preventDefault()
      // ajl = ajax listener, обработка результатам ajax-реквеста.
      val ajl: (Any, String, JQueryXHR) => js.Any = {
        (data, textStatus, jqXHR) =>
          val tgList = jQuery("#" + ID_ALL_TARGETS_LIST)
          tgList.append(data)
          // Фокус на поле URL.
          tgList.children()
            .last()
            .find("input[name='tg.url']")
            .focus()
      }
      val el = jQuery(evt.currentTarget)
      val href = el.attr("href")
      // Запустить ajax-реквест на исполнение.
      jQuery.ajax(js.Dictionary[js.Any](
        "method"  -> "GET",
        "url"     -> href,
        "success" -> ajl
        // TODO Добавить обработку ошибок.
      ).asInstanceOf[JQueryAjaxSettings])
    }
    // Повесить обработчик клика на кнопку добавления новой цели.
    jQuery("#" + ID_ADD_TARGET_LINK)
      .click(listener: js.Any)
  }


  /** Событие удаления цели со страницы. */
  private def bindDeleteTargetClick(): Unit = {
    val listener = { evt: JQueryEventObject =>
      evt.preventDefault()
      val ct = jQuery(evt.currentTarget)
      val delHref = ct.attr("href")
      val oneTgDiv = ct.parents("." + CLASS_ONE_TARGET_CONTAINER)
      val onSuccess = {() => oneTgDiv.remove() }
      if (delHref == "#") {
        // Это новая форма, которую юзер удаляет без сохранения.
        onSuccess()
      } else {
        // Это форма, сохраненная ранее. Собрать листенер результата асинхронного запроса.
        // Собрать ajax-запрос и отправить на указанный URL.
        val ajaxSettings = js.Dictionary[js.Any](
          "method"  -> "POST",
          "url"     -> delHref,
          "statusCode" -> {
            type T = js.Function0[_]
            val onSuccess1: T = onSuccess
            js.Dictionary[T](
              "204" -> onSuccess1,
              "404" -> onSuccess1,
              "403" -> {() => dom.alert("Please try again (error http 403).") }
            )
          }
        )
        jQuery.ajax( ajaxSettings.asInstanceOf[JQueryAjaxSettings] )
      }
    }
    // Делегировать обработчик удаления списку всех целей.
    jQuery("#" + ID_ALL_TARGETS_LIST)
      .on("click",  "." + CLASS_DELETE_TARGET_BTN,  listener: js.Any)
  }


  /** Событие изменения содержимого одного из инпутов в форме одной цели. */
  private def bindOneTgChange(): Unit = {
    val listener = {evt: JQueryEventObject =>
      val input = jQuery(evt.currentTarget)
      val tgForm = input.parents("." + CLASS_ONE_TARGET_FORM_INNER)
      tgForm.trigger("submit")
    }
    // Повесить листенеры на инпуты
    jQuery("#" + ID_ALL_TARGETS_LIST)
      .on("change",  "." + CLASS_ONE_TARGET_INPUT,  listener : js.Any)
  }


  /** При сабмите формы редактирования одной цели, нужно подменять обработку. */
  private def bindOneTgSubmit(): Unit = {
    val listener = { evt: JQueryEventObject =>
      evt.preventDefault()
      val oldForm = jQuery(evt.currentTarget)
      val oldFormInputs = oldForm.find("input")
      // Перед блокировкой полей формы надо сграбить с них все данные.
      // TODO Нужно избегать потери фокуса при переключении между полями.
      val data = oldFormInputs.serialize()
      // На время сабмита нужно заблокировать поля ввода текущей формы
      oldFormInputs.prop("disabled", true)
      // onComplete() вызывается при ответе на запрос по любому ожидаемому коду.
      def onComplete(respBody: js.Any): Unit = {
        // Отреплейсить содержимое формы.
        oldForm.html( jQuery(respBody).html() )     // TODO Можно тут выпилить вызов jQuery()?
        //oldFormInputs.prop("disabled", false)     // TODO это надо вообще? Форма же перезаписывается новой.
      }
      val ajaxSettings = js.Dictionary[js.Any](
        "method"      -> "POST",
        "url"         -> oldForm.attr("action"),
        "data"        -> data,
        "statusCode"  -> js.Dictionary[js.Any](
          "406" -> { (resp: XMLHttpRequest) =>
            onComplete( resp.responseText )
          }
          // TODO отрабатывать другие ошибки, например истекшую сессию и редирект в ответе.
        ),
        "success" -> { (respBody: js.Any) =>
          onComplete(respBody)
        }
      )
      jQuery.ajax( ajaxSettings.asInstanceOf[JQueryAjaxSettings] )
    }
    // Повесить обработчик на все формы редактирования
    jQuery("#" + ID_ALL_TARGETS_LIST)
      .on("submit", "." + CLASS_ONE_TARGET_FORM_INNER, listener: js.Any)
  }


  /** Сабмит формы размещения целей. Нужно залить в форму необходимые инпуты и продолжить выполнение. */
  private def bindAdvFormSubmit(): Unit = {
    val listener = { evt: JQueryEventObject =>
      val newTags = jQuery("#" + ID_ALL_TARGETS_LIST)
        .find("." + CLASS_ONE_TARGET_CONTAINER)
        // Формы с галочками превратить в input-теги.
        .map { (indexRaw: js.Any, oneTgDiv: Element) =>
          val oneTg = jQuery(oneTgDiv)
          val retInputs = oneTg.find("input[name=return]:checked")
          if (retInputs.length > 0) {
            val index = indexRaw.asInstanceOf[Int]
            val namePrefix = "adv[" + index + "]."
            // Собрать тег return
            val retInput = retInputs(0)
            val retTag = dom.document.createElement("input")
            retTag.setAttribute("type",  "hidden")
            retTag.setAttribute("name",  namePrefix + "return")
            retTag.setAttribute("value", retInput.getAttribute("value"))
            // Собрать тег id
            val idTag = dom.document.createElement("input")
            idTag.setAttribute("type",  "hidden")
            idTag.setAttribute("name",  namePrefix + "tg_id")
            idTag.setAttribute("value", oneTg.find("input[name='tg.id']")(0).getAttribute("value") )
            // Вернуть flatMap()-результат
            js.Array(idTag, retTag)

          } else {
            // Пропускаем текущую форму, т.к. галочки не выставлены.
            null
          }
        }
      // Когда теги готовы, то безвозвратно добавить их в форму, у которой происходит сабмит.
      jQuery(evt.currentTarget).prepend(newTags)
    }
    // Вешаем данный обработчик на целевую форму
    jQuery("#" + ID_ADV_FORM)
      .on("submit", listener: js.Any)
  }


  /** Чекбоксы в рамках одного div'а должны имитировать поведение radiogroup. */
  private def bindReturnCheckboxRadio(): Unit = {
    // Есть мнение, что вынос cbSel за скобки не сказывается положительно на потреблении ресурсов.
    val cbSel = "." + CLASS_ONE_TARGET_INPUT_RETURN_CHECKBOX
    val listener = { evt: JQueryEventObject =>
      val cb = jQuery(evt.currentTarget)
      val cbChecked = Option(cb.prop("checked")).fold(false)(_.asInstanceOf[Boolean])
      if (cbChecked) {
        cb.parents("." + CLASS_ONE_TARGET_CONTAINER)
          .find(cbSel)
          .not(cb)
          .prop("checked", false)
      }
    }
    // Делегируем обработчик контейнеру списка целей
    jQuery("#" + ID_ALL_TARGETS_LIST)
      .on("change", cbSel, listener: js.Any)
  }

}
