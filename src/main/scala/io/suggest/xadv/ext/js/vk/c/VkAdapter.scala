package io.suggest.xadv.ext.js.vk.c

import java.net.URI

import io.suggest.xadv.ext.js.runner.c.IActionContext
import io.suggest.xadv.ext.js.runner.c.adp.AsyncInitAdp
import io.suggest.xadv.ext.js.runner.m.ex._
import io.suggest.xadv.ext.js.runner.m._
import io.suggest.xadv.ext.js.vk.c.hi.Vk
import io.suggest.xadv.ext.js.vk.m._
import org.scalajs.dom
import io.suggest.xadv.ext.js.vk.m.VkWindow._
import scala.scalajs.concurrent.JSExecutionContext

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 15:10
 * Description: Клиент-адаптер для вконтакта.
 */

object VkAdapter {

  /** bitmask разрешений на доступ к API. */
  private def ACCESS_LEVEL = 8196

  /** Относится ли указанный домен к текущему клиенту? */
  def isMyDomain(domain: String): Boolean = {
    domain matches "(www\\.)?vk(ontakte)?\\.(ru|com)"
  }

}


import VkAdapter._


class VkAdapter extends AsyncInitAdp {

  override type Ctx_t = MJsCtxT

  override def SCRIPT_URL: String = "https://vk.com/js/api/openapi.js"
  override def SCRIPT_LOAD_TIMEOUT_MS = 6000

  /** Относится ли указанный домен к текущему клиенту? */
  override def isMyDomain(domain: String): Boolean = {
    VkAdapter.isMyDomain(domain)
  }

  override def setInitHandler(handler: () => _): Unit = {
    dom.window.vkAsyncInit = handler
  }

  override implicit def execCtx: ExecutionContext = JSExecutionContext.queue

  /** Добавить тег скрипта по особой уличной методике вконтакта. */
  override def addScriptTag(): Unit = {
    println("addScriptTag() vk")
    // Требуется создавать этот div. Пруф https://vk.com/dev/openapi
    val div = dom.document.createElement("div")
    div.setAttribute("id", "vk_api_transport")
    div.appendChild(createScriptTag())
    appendScriptTag(div)
  }

  /**
   * Инициализация адаптера и данных по юзеру.
   * Вызывается, когда нижележащее API уже инициализировано и готово.
   * @return Фьючерс с новым VkCtx.
   */
  override def serviceScriptLoaded(implicit actx: IActionContext): Future[Ctx_t] = {
    val apiId = actx.mctx0
      .service
      .flatMap(_.appId)
      .orNull
    val initFut = Vk.init( VkInitOptions(apiId) )
    // Начальная инициализация vk openapi.js вроде бы завершена. Можно узнать на тему залогиненности клиента.
    val lsFut = initFut flatMap { _ =>
      Vk.Auth.getLoginStatus
    } recover { case ex: Throwable =>
      // Подавляем любые ошибки, т.к. эта функция в общем некритична, хоть и намекает на неработоспособность API.
      dom.console.warn("VK Cannot getLoginStatus(): %s: %s", ex.getClass.getSimpleName, ex.getMessage)
      None
    }
    // В фоне запускаем получение текущих прав приложения, хоть они могут и не пригодится.
    val appPermsFut = initFut flatMap { _ =>
      Vk.Api.getAppPermissions()
    }
    appPermsFut onFailure { case ex: Throwable =>
      dom.console.warn("vk.api.getPermissions() failed: " + ex.getClass.getName + ": " + ex.getMessage)
    }
    val ls2Fut = lsFut flatMap {
      // Залогиненный юзер, но у приложения может не хватать прав, что позволит считать его незалогиненным.
      case lsSome if lsSome.isDefined =>
        // Возможно, юзер ранее уже подтвердил все необходмые права доступа приложению. И только тогда считаем его залогиненным.
        appPermsFut
          // Отмаппить полученные права на простое "да/нет".
          .map { appPerms =>
            (appPerms.bitMask & ACCESS_LEVEL) == ACCESS_LEVEL
          }
          // Подавляем возможные ошибки, т.к. эта проверка некритична.
          .recover { case ex: Throwable =>
            dom.console.warn("VK Cannot getAppPermissions(): %s: %s", ex.getClass.getName, ex.getMessage)
            false
          }
          // Генеря результат, мы определяем необходимо ли вызывать login() на след.шаге.
          .map { hasPerms =>
            lsSome.filter(_ => hasPerms)
          }

      // Это незалогиненный юзер или произошла ошика getLoginStatus. login() будет вызван на след.шаге.
      case None =>
        Future successful None
    }
    // Собираем контекст
    ls2Fut map { lsOpt =>
      actx.mctx0.copy(
        status = Some( MAnswerStatuses.Success ),
        custom = Some( VkCtx(login = lsOpt).toJson )
      )
    }
  }


  /** Запуск обработки одной цели. */
  override def handleTarget(implicit actx: IActionContext): Future[MJsCtx] = {
    val mctx0 = actx.mctx0
    loggedIn [MJsCtx](mctx0) { vkCtx =>
      // Публикация идёт в два шага: загрузка картинки силами сервера s.io и публикация записи с картинкой.
      val savedOpt = mctx0.mads
        .headOption
        .flatMap(_.picture)
        .flatMap(_.saved)
      if (savedOpt.isDefined) {
        dom.console.log("vk.handleTarget() picture already uploaded. publishing.")
        step2(vkCtx)

      } else if (mctx0.mads.nonEmpty) {
        dom.console.log("vk.handleTarget(): Requesing s2s pic upload.")
        step1(vkCtx)

      } else {
        // Should never happen. Нет карточек для размещения.
        Future failed AdapterFatalError(MServices.VKONTAKTE, "All mctx.mads looks lost.")
      }
    }
  }

  protected def runLogin()(implicit actx: IActionContext): Future[VkLoginResult] = {
    actx.app.popupQueue.enqueue { () =>
      Vk.Auth.login(ACCESS_LEVEL)
    } flatMap {
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
  protected def loggedIn[T](mctx0: MJsCtx)(f: VkCtx => Future[T])(implicit actx: IActionContext): Future[T] = {
    val vkCtxOpt = VkCtx.maybeFromDyn(mctx0.custom)
    val loginOpt = vkCtxOpt.flatMap(_.login)
    loginOpt match {
      // Юзер не залогинен. Запустить процедуру логина.
      case None =>
        runLogin().flatMap { loginCtx =>
          val some = Some(loginCtx)
          val vkCtx1 = vkCtxOpt match {
            // Залить новую инфу по логину во внутренний контекст
            case Some(vkCtx)  =>   vkCtx.copy(login = some)
            // should never happen
            case None         =>   VkCtx(login = some)
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
   * @return Фьючерс с новым контекстом.
   */
  protected def step1(vkCtx: VkCtx)(implicit actx: IActionContext): Future[MJsCtx] = {
    import actx.mctx0
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
   * @param vkCtx Исходный внутренний контекст vk-адаптера.
   * @return Фьючерс с новым контекстом.
   */
  protected def step2(vkCtx: VkCtx)(implicit actx: IActionContext): Future[MJsCtx] = {
    import actx.mctx0
    saveWallPhotos(mctx0, vkCtx)
      .flatMap { mkWallPost(vkCtx, _) }
      .map { postRes =>
        // TODO Передать на сервер ссылку или иные данные, присланные в результате.
        mctx0.copy(
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
  protected def mkWallPost(vkCtx: VkCtx, photos: Seq[VkSaveWallPhotoResult])
                          (implicit actx: IActionContext): Future[VkWallPostResult] = {
    import actx.mctx0
    val ocUrl = mctx0.target.get.onClickUrl
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
      mctx0.mads.headOption
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
    // Отобразить vk-попап подтверждения постинга и обработать результат.
    actx.app.popupQueue.enqueue { () =>
      Vk.Api.wallPost(postArgs)
    } flatMap { res =>
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
