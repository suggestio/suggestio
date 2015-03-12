package io.suggest.xadv.ext.js.vk.c

import java.net.URI

import io.suggest.xadv.ext.js.runner.m.ex._
import io.suggest.xadv.ext.js.runner.m._
import io.suggest.xadv.ext.js.vk.c.hi.Vk
import io.suggest.xadv.ext.js.vk.m._
import org.scalajs.dom
import io.suggest.xadv.ext.js.vk.m.VkWindow._

import scala.concurrent.{Promise, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 15:10
 * Description: Клиент-адаптер для вконтакта.
 */

object VkAdapter {

  /** bitmask разрешений на доступ к API. */
  private def ACCESS_LEVEL = 8197

  /** Относится ли указанный домен к текущему клиенту? */
  def isMyDomain(domain: String): Boolean = {
    domain matches "(www\\.)?vk(ontakte)?\\.(ru|com)"
  }

  private def SCRIPT_LOAD_TIMEOUT_MS = 6000

  private def SCRIPT_URL = "https://vk.com/js/api/openapi.js"

  /** Добавить необходимые теги для загрузки. Максимум один раз. */
  private def addOpenApiScript(): Unit = {
    // Создать div для добавления туда скрипта. Так зачем-то было сделано в оригинале.
    val div = dom.document.createElement("div")
    val divName = "vk_api_transport"
    div.setAttribute("id", divName)
    dom.document
      .getElementsByTagName("body")(0)
      .appendChild(div)

    // Создать тег скрипта и добавить его в свежесозданный div
    val el = dom.document.createElement("script")
    el.setAttribute("type",  "text/javascript")
    // Всегда долбимся на https. Это работает без проблем с file:///, а на мастере всегда https.
    el.setAttribute("src", SCRIPT_URL)
    el.setAttribute("async", true.toString)
    dom.document
      .getElementById(divName)
      .appendChild(el)
  }
}


import VkAdapter._


class VkAdapter extends IAdapter {

  /** Относится ли указанный домен к текущему клиенту? */
  override def isMyDomain(domain: String): Boolean = {
    VkAdapter.isMyDomain(domain)
  }

  /** Запуск инициализации клиента. Добавляется необходимый js на страницу. */
  override def ensureReady(mctx0: MJsCtx): Future[MJsCtx] = {
    val p = Promise[MJsCtx]()
    // Создать обработчик событие инициализации.
    val window: VkWindow = dom.window
    window.vkAsyncInit = {() =>
      val apiId = mctx0.service.get.appId.orNull
      val opts = VkInitOptions(apiId)
      Vk.init(opts).flatMap { _ =>
        // Начальная инициализация vk openapi.js вроде бы завершена. Можно узнать на тему залогиненности клиента.
        // TODO Проверить пермишшены через account.getAppPermissions()
        Vk.Auth.getLoginStatus
      } onComplete {
        // init завершился, инфа по залогиненности получена.
        case Success(loginStatusOpt) =>
          val vkCtx = VkCtx(
            login = loginStatusOpt
          )
          p success mctx0.copy(
            status = Some(MAnswerStatuses.Success),
            custom = Some(vkCtx.toJson)
          )
        // Какая-то из двух операций не удалась. Не важно какая -- суть одна: api не работает.
        case Failure(ex) =>
          p failure ApiInitException(ex)
      }
      // Освободить память браузера от хранения этой функции.
      window.vkAsyncInit = null
    }
    // Добавить тег со ссылкой на open-api. Это запустит процесс в фоне.
    Future {
      addOpenApiScript()
    } onFailure { case ex =>
      p failure DomUpdateException(ex)
    }
    // Отрабатываем таймаут загрузки скрипта вконтакта
    dom.setTimeout(
      {() =>
        // js однопоточный, поэтому никаких race conditions между двумя нижеследующими строками тут быть не может:
        if (!p.isCompleted)
          p failure UrlLoadTimeoutException(SCRIPT_URL, SCRIPT_LOAD_TIMEOUT_MS)
      },
      SCRIPT_LOAD_TIMEOUT_MS
    )
    // Вернуть фьючерс результата
    p.future
  }



  /** Запуск обработки одной цели. */
  override def handleTarget(mctx0: MJsCtx): Future[MJsCtx] = {
    loggedIn [MJsCtx](mctx0) { vkCtx =>
      // Публикация идёт в два шага: загрузка картинки силами сервера s.io и публикация записи с картинкой.
      if (mctx0.mads.headOption.flatMap(_.picture).flatMap(_.saved).isDefined) {
        dom.console.log("vk.handleTarget() picture already uploaded. publishing.")
        step2(mctx0, vkCtx)

      } else if (mctx0.mads.nonEmpty) {
        dom.console.log("vk.handleTarget(): Requesing s2s pic upload.")
        step1(mctx0, vkCtx)

      } else {
        // Should never happen. Нет карточек для размещения.
        Future failed AdapterFatalError(MServices.VKONTAKTE, "All mctx.mads looks lost.")
      }
    }
  }

  protected def runLogin(): Future[VkLoginResult] = {
    Vk.Auth.login(ACCESS_LEVEL) flatMap {
      case None =>
        Future failed LoginCancelledException()
      case Some(login) =>
        Future successful login
    }
  }

  /**
   * Произвести вызов указанного callback'a, предварительно убедившись, что юзер залогинен.
   * @param mctx0 Исходный контекст.
   * @param f callback. Вызывается когда очевидно, что юзер залогинен.
   * @tparam T Тип результата callback'а и этого метода.
   * @return Фьючерс с результатом callback'а или ошибкой.
   */
  protected def loggedIn[T](mctx0: MJsCtx)(f: VkCtx => Future[T]): Future[T] = {
    val vkCtxOpt = VkCtx.maybeFromDyn(mctx0.custom)
    val loginOpt = vkCtxOpt.flatMap(_.login)
    loginOpt match {
      // Юзер не залогинен. Запустить процедуру логина.
      case None =>
        runLogin().flatMap { loginCtx =>
          val some = Some(loginCtx)
          val vkCtx1 = vkCtxOpt match {
            case Some(vkCtx) =>
              // Залить новую инфу по логину во внутренний контекст
              vkCtx.copy(login = some)
            case None =>
              // should never happen
              VkCtx(login = some)
          }
          f(vkCtx1)
        }

      // Юзер залогинен уже. Сразу дергаем callback.
      case _ =>
        f(vkCtxOpt.get)
    }
  }


  /**
   * Извлечь screen-имя из ссылки на страницу-цель размещения.
   * @param url Ссылка на страницу-цель размещения.
   * @return Опциональный результат.
   */
  protected def extractScreenName(url: String): Option[String] = {
    val path = new URI(url).getPath
    val regex = "^/([_a-zA-Z0-9-]{1,64}).*".r
    path match {
      case regex(screenName) => Some(screenName)
      case _ => None
    }
  }

  protected def getTargetVkId(screenNameOpt: Option[String], vkCtx: VkCtx): Future[VkTargetInfo] = {
    screenNameOpt match {
      case Some(screenName) =>
        val args = VkResolveScreenNameArgs(screenName)
        val sname = Vk.Api.resolveScreenName(args)
        // Сразу запускаем проверку прав на постинг.
        sname.flatMap(ensureCanPostInto)

      case None =>
        val vti = VkTargetInfo(
          id      = vkCtx.login.get.vkId,
          tgType  = VkTargetTypes.User,
          name    = None
        )
        Future successful vti
    }
  }

  /**
   * Асинхронно проверить возможность постинга на указанную vk-цель (страницу).
   * @param vktg Инфа по таргету.
   * @return Future.successful с исходником.
   *         Future.failed если постить нельзя.
   */
  protected def ensureCanPostInto(vktg: VkTargetInfo): Future[VkTargetInfo] = {
    if (vktg.tgType.isGroup) {
      // Надо проверить, есть ли права на постинг в эту группу.
      val grArgs = VkGroupGetByIdArgs(groupId = vktg.id, fields = VkGroupGetByIdArgs.CAN_POST_FN)
      Vk.Api.groupGetById(grArgs) flatMap { res =>
        if (res.canPost.exists(identity) && res.deactivated.isEmpty) {
          val vktg1 = vktg.copy(
            name = Some(res.name)
          )
          Future successful vktg1
        } else {
          Future failed PostingProhibitedException(res.name, info = Some(res.toString))
        }
      }

    } else {
      // Это не группа. Проверять права на другие объекты заранее не умеем.
      Future successful vktg
    }
  }


  /**
   * Первый шаг постинга на стену.
   * Метод производит действия, связанные с загрузкой картинки в хранилище внешнего сервиса.
   *
   * Подготовка к публикации идёт в несколько шагов:
   * - Извлечение имени из url.
   * - Резолвинг имени в vk id.
   * - Проверка прав на постинг в запрошенную цель.
   * - Получения url сервера для upload POST.
   * - Отправка нового контекста на сервер.
   * Возможен так же вариант, когда нет прав на постинг на указанную страницу.
   * @param mctx0 Исходный контекст.
   * @return Фьючерс с новым контекстом.
   */
  protected def step1(mctx0: MJsCtx, vkCtx: VkCtx): Future[MJsCtx] = {
    // Извлечь имя из target.url
    val tg = mctx0.target.get
    val screenNameOpt = extractScreenName(tg.tgUrl)
    // Отрезолвить цель
    val vtiFut = getTargetVkId(screenNameOpt, vkCtx)
    // Узнать url для POST'а картинки.
    val gwusFut = vtiFut
      .map { vti => VkPhotosGetWallUploadServerArgs(vti.id) }
      .flatMap { Vk.Api.photosGetWallUploadServer }
    // Когда всё готово, заварганить команду для сервера в контексте:
    for {
      vti <- vtiFut
      gwus <- gwusFut
    } yield {
      // Залить ссылку в контекст.
        val ulCtx = MPicS2sUploadCtx(
          url = gwus.uploadUrl,
          partName = "photo"
        )
        val mads1 = mctx0.mads.map { mad =>
          val someUlCtx = Some(ulCtx)
          val pic1 = mad.picture match {
            case Some(ctx) => ctx.copy(upload = someUlCtx)
            case None      => MAdPictureCtx(upload = someUlCtx)
          }
          mad.copy(picture = Some(pic1))
        }
        val vkCtx1 = vkCtx.copy(
          tgInfo = Some(vti)
        )
        mctx0.copy(
          mads   = mads1,
          status = Some(MAnswerStatuses.FillContext),
          custom = Some(vkCtx1.toJson)
        )
      }
  }


  /**
   * Это шаг 2 (второй вызов handleTarget() сервером sio).
   * Здесь нужно на основе данных в контексте нужно сформировать запрос присоединения загруженной картинки
   * к стене цели, и затем произвести публикацию записи на стене.
   * @param mctx Исходный контекст.
   * @param vkCtx Исходный внутренний контекст vk-адаптера.
   * @return Фьючерс с новым контекстом.
   */
  protected def step2(mctx: MJsCtx, vkCtx: VkCtx): Future[MJsCtx] = {
    saveWallPhotos(mctx, vkCtx)
      .flatMap { mkWallPost(mctx, vkCtx, _) }
      .map { postRes =>
        // TODO Передать на сервер ссылку или иные данные, присланные в результате.
        mctx.copy(
          status = Some(MAnswerStatuses.Success)
        )
      }
  }


  /** Прилинковать загруженную картинку к стене юзера. */
  protected def saveWallPhotos(mctx: MJsCtx, vkCtx: VkCtx): Future[Seq[VkSaveWallPhotoResult]] = {
    val tgId = vkCtx.tgInfo.get.id
    // TODO Надо ли усилить контроль порядка результирующей коллекции?
    Future.traverse( mctx.mads ) { mad =>
      val swpArgs = mad.picture
        .flatMap(_.saved)
        .map { VkSaveWallPhotoArgs(tgId, _) }
        .get
      Vk.Api.saveWallPhoto(swpArgs)
    }
  }

  /** Запостить сообщение на стену на основе собранных данных. */
  protected def mkWallPost(mctx: MJsCtx, vkCtx: VkCtx, photos: Seq[VkSaveWallPhotoResult]): Future[VkWallPostResult] = {
    val ocUrl = mctx.target.get.onClickUrl
    // Готовим содержимое картинки.
    val attachments = {
      val photoIdsIter = photos.iterator.map { _.id }
      (photoIdsIter ++ Iterator(ocUrl)).toSeq
    }
    // Готовим целевого адресата
    val tgInfo = vkCtx.tgInfo.get
    val ownerId = if (tgInfo.tgType.isGroup)
      -tgInfo.id
    else
      tgInfo.id
    // Текст надписи на стене под картинкой.
    val msg = {
      mctx.mads.headOption
        .flatMap { mad0 => mad0.content.descr }
        .iterator
        .mkString("\n")
    }
    // Объединяем все данные для поста.
    val postArgs = VkWallPostArgs(
      ownerId     = ownerId,
      attachments = attachments,
      message     = Some(msg)
    )
    Vk.Api.wallPost(postArgs) flatMap { res =>
      // Если сервер вернул ошибку, то закончить на этом.
      if (res.error.isDefined) {
        Future failed PostingProhibitedException(
          tgTitle = tgInfo.name getOrElse tgInfo.id.toString,
          info = res.error
        )
      } else {
        Future successful res
      }
    }
  }

}
