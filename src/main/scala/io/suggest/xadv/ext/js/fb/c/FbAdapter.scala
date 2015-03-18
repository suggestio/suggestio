package io.suggest.xadv.ext.js.fb.c

import io.suggest.xadv.ext.js.fb.c.hi.Fb
import io.suggest.xadv.ext.js.fb.m._
import io.suggest.xadv.ext.js.runner.m.ex._
import io.suggest.xadv.ext.js.runner.m.{MAnswerStatuses, MJsCtx, IAdapter}
import org.scalajs.dom

import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 17:24
 * Description: Адаптер для постинга в facebook.
 */
object FbAdapter {

  /** Таймаут загрузки скрипта. */
  def SCRIPT_LOAD_TIMEOUT = 10000

  /** Ссылка на скрипт. */
  def SCRIPT_URL = "https://connect.facebook.net/en_US/sdk.js"

  /** Добавить тег со скриптом загрузки facebook js sdk. */
  private def addScriptTag(): Unit = {
    val d = dom.document
    // TODO Надо ли вообще проверять наличие скрипта?
    // У макса была какая-то костыльная проверка, сюда она перекочевала в упрощенном виде.
    val id = "facebook-jssdk"
    if (d.getElementById(id) == null) {
      val tag = d.createElement("script")
      tag.setAttribute("async", true.toString)
      tag.setAttribute("type", "text/javascript")
      tag.setAttribute("src", SCRIPT_URL)
      d.getElementsByTagName("body")(0)
        .appendChild(tag)
    }
  }

}


import FbAdapter._


class FbAdapter extends IAdapter {

  /** Относится ли указанный домен к текущему клиенту? */
  override def isMyDomain(domain: String): Boolean = {
    domain matches "(www\\.)?facebook.(com|net)"
  }

  /** Запуск инициализации клиента. Добавляется необходимый js на страницу,  */
  override def ensureReady(mctx0: MJsCtx): Future[MJsCtx] = {
    val p = Promise[MJsCtx]()
    // Подписаться на событие загрузки скрипта.
    val window: FbWindow = dom.window
    window.fbAsyncInit = { () =>
      val appId = mctx0.service.get.appId.orNull
      val opts = FbInitOptions(appId)
      Fb.init(opts) onComplete {
        // Инициализация удалась.
        case Success(_) =>
          p success mctx0.copy(
            status = Some(MAnswerStatuses.Success)
          )
        // Возник облом при инициализации.
        case Failure(ex) =>
          p failure ApiInitException(ex)
      }
      // Вычищаем эту функцию из памяти браузера.
      window.fbAsyncInit = null
    }
    // Добавить скрипт facebook.js на страницу
    try {
      addScriptTag()
    } catch {
      case ex: Throwable =>
        p failure DomUpdateException(ex)
    }
    // Среагировать на слишком долгую загрузку скрипта таймаутом.
    dom.setTimeout(
      {() =>
        if (!p.isCompleted)
          p failure UrlLoadTimeoutException(SCRIPT_URL, SCRIPT_LOAD_TIMEOUT)
      },
      SCRIPT_LOAD_TIMEOUT
    )
    p.future
  }

  /**
   * Запуск логина и обрабока ошибок.
   * @return Фьючерс с выверенным результатом логина.
   */
  protected def doLogin(): Future[FbLoginResult] = {
    val args = FbLoginArgs(
      scope = FbLoginArgs.ALL_RIGHTS
    )
    Fb.login(args) flatMap { res =>
      if (res.hasAuthResp)
        Future successful res
      else
        Future failed LoginCancelledException()
    }
  }

  /** Сборка и публикация поста. */
  protected def publishPost(mctx0: MJsCtx, tgInfo: IFbPostingInfo): Future[_] = {
    // TODO Заимплеменчена обработка только первой карточки
    val mad = mctx0.mads.head
    val args = FbPost(
      picture     = mad.picture.flatMap(_.url),
      message     = None, //Some( mad.content.fields.iterator.map(_.text).mkString("\n") ),
      link        = Some( mctx0.target.get.onClickUrl ),
      name        = mad.content.title,
      descr       = mad.content.descr,
      accessToken = tgInfo.accessToken
    )
    Fb.mkPost(tgInfo.nodeId, args)
      .flatMap { res =>
        res.error match {
          case some if some.isDefined =>
            Future failed PostingProhibitedException(tgInfo.nodeId, some)
          case _ =>
            Future successful res
        }
      }
  }

  /**
   * Узнать тип указанной ноды через API.
   * @param fbId id узла.
   * @return None если не удалось определить id узла.
   *         Some с данными по цели, которые удалось уточнить. Там МОЖЕТ БЫТЬ id узла.
   */
  protected def getNodeType(fbId: String): Future[Option[FbTarget]] = {
    val args = FbNodeInfoArgs(
      id        = fbId,
      fields    = FbNodeInfoArgs.FIELDS_ONLY_ID,
      metadata  = true
    )
    Fb.getNodeInfo(args)
      .map { resp =>
        val ntOpt = resp.metadata
          .flatMap(_.nodeType)
          .orElse {
            // Если всё еще нет типа, но есть ошибка, то по ней бывает можно определить тип.
            resp.error
              .filter { e => e.code contains 803 }    // Ошибка 803 говорит, что у юзера нельзя сдирать инфу.
              .map { _ => FbNodeTypes.User }
          }
        if (ntOpt.isEmpty)
          dom.console.warn("Unable to get node type from getNodeInfo(%s) resp %s", args.toString, resp.toString)
        val res = FbTarget(
          nodeId        = resp.nodeId.getOrElse(fbId),
          nodeType  = ntOpt
        )
        Some(res)
      // recover() для подавления любых возможных ошибок.
      }.recover {
        case ex: Throwable =>
          dom.console.warn("Failed to getNodeInfo(%s) or parse result: %s %s", args.toString, ex.getClass.getSimpleName, ex.getMessage)
          None
      }
  }

  /**
   * Получить access_token для постинга на указанный узел.
   * @param fbId id узла, для которого требуется токен.
   * @return None если не удалось получить токен.
   *         Some с токеном, когда всё ок.
   */
  protected def getAccessToken(fbId: String): Future[Option[String]] = {
    val args = FbNodeInfoArgs(
      id          = fbId,
      fields      = Seq(FbNodeInfoArgs.ACCESS_TOKEN_FN),
      metadata    = false
    )
    Fb.getNodeInfo(args)
      .map { resp =>
        // Если нет токена в ответе, то скорее всего там указана ошибка. Её рендерим в логи для отладки.
        if (resp.error.isDefined)
          dom.console.warn("Failed to acquire access token: %s => error %s", args.toString, resp.error.toString)
        resp.accessToken
      }
  }

  /** Запуск обработки одной цели. */
  override def handleTarget(mctx0: MJsCtx): Future[MJsCtx] = {
    // TODO Нужно оформить код как полноценный FSM с next_state. Логин вызывать однократно в ensureReady() в blocking-режиме.
    doLogin() flatMap { _ =>
      // Первый шаг - определяем по узлу данные.
      val tgInfoOpt = FbTarget.fromUrl( mctx0.target.get.tgUrl )
      tgInfoOpt match {
        // Неизвестный URL. Завершаемся.
        case None =>
          Future failed new UnknownTgUrlException
        case Some(info) =>
          if (info.nodeType.isEmpty) {
            // id узла есть, но тип не известен. Нужно запустить определение типа указанной цели.
            getNodeType(info.nodeId)
              .map { _ getOrElse info }
          } else {
            Future successful info
          }
      }

    } flatMap { fbTg =>
      // Второй шаг: Запрашиваем специальный access_token, если требуется.
      if (fbTg.nodeType contains FbNodeTypes.Page) {
        // Для постинга на страницу нужно запросить page access token.
        getAccessToken(fbTg.nodeId)
          .map { pageAcTokOpt =>
            FbTgPostingInfo(nodeId = fbTg.nodeId, accessToken = pageAcTokOpt)
          }
      } else {
        val res = FbTgPostingInfo(nodeId = fbTg.nodeId, accessToken = None)
        Future successful res
      }

    } flatMap { tgInfo =>
      // Третий шаг - запуск публикации поста.
      publishPost(mctx0, tgInfo)

    } map { res =>
      // Если всё ок, вернут результат.
      mctx0.copy(
        status = Some(MAnswerStatuses.Success)
      )
    }
  }

}
