package io.suggest.xadv.ext.js.fb.c

import io.suggest.xadv.ext.js.fb.c.hi.Fb
import io.suggest.xadv.ext.js.fb.m._
import io.suggest.xadv.ext.js.runner.c.IActionContext
import io.suggest.xadv.ext.js.runner.m.ex._
import io.suggest.xadv.ext.js.runner.m._
import org.scalajs.dom
// В целях оптимизации нижеследующие, почти все синхронные, операции над фьючерсом объединяются через runNow.
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 17:24
 * Description: Адаптер для постинга в facebook.
 */
object FbAdapter {

  /** Таймаут загрузки скрипта. */
  def SCRIPT_LOAD_TIMEOUT_MS = 10000

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

  /** Инициализация системы  */
  protected def headlessInit(mctx0: MJsCtx): Future[FbCtx] = {
    // Запускаем инициализацию FB API.
    val appId = mctx0.service
      .flatMap(_.appId)
      .orNull
    val opts = FbInitOptions(appId)
    val initFut = Fb.init(opts)

    // Сразу запрашиваем инфу по fb-залогиненности текущего юзера и права доступа.
    val loginStatusFut = initFut flatMap { _ =>
      Fb.getLoginStatus()
    }

    // Когда появится инфа о залогиненном юзере, то нужно запросить текущие пермишшены.
    val permissionsFut = loginStatusFut
      // Анализировать новых юзеров смысла нет
      .filter { _.status.isAppConnected }
      // У известного юзера надо извлечь данные о текущих пермишшенах и его fb userID.
      .map { ls =>
        for {
          authResp  <- ls.authResp
          userId    <- authResp.userId
          atok      <- authResp.accessToken
        } yield {
          (userId, atok)
        }
      }
      // Если там ничего нет, то тоже приостановить обработку.
      .map { _.get }
      // Если есть текущий токен и userId, то воспользоваться ими для получения списка текущих прав.
      .flatMap { case (userId, atok) =>
        val permArgs = FbGetPermissionsArgs(userId = userId, accessToken = Some(atok))
        Fb.getPermissions(permArgs)
          .map { resp =>
            resp.data
              .iterator
              .filter { _.status.isGranted }    // Нужны только заапрувленные права
              .map { _.permission }             // Отбросить состояние права, оставить только само право.
              .toSeq
          }
      }
      // Если обработка была приостановлена, то вернуть пустой список прав.
      .recover {
        case ex: NoSuchElementException =>
          Nil
        case ex: Throwable =>
          dom.console.warn("FbAdapter.headlessInit(): Failed to request fb permissions: %s: %s", ex.getClass.getName, ex.getMessage)
          Nil
      }

    // Формируем и возвращаем результирующий внутренний контекст.
    for {
      hasPerms <- permissionsFut
    } yield {
      FbCtx(
        hasPerms = hasPerms
      )
    }
  }


  /** Запуск инициализации клиента. Добавляется необходимый js на страницу,  */
  override def ensureReady(implicit actx: IActionContext): Future[MJsCtx] = {
    val mctx0 = actx.mctx0
    val p = Promise[MJsCtx]()
    // Подписаться на событие загрузки скрипта.
    val window: FbWindow = dom.window
    window.fbAsyncInit = { () =>
      // Запускаем инициализацию.
      headlessInit(mctx0) andThen {
        // Инициализация удалась
        case Success(fbctx) =>
          p success mctx0.copy(
            status = Some(MAnswerStatuses.Success),
            custom = Some(fbctx.toJson)
          )
        // Возник облом при инициализации.
        case Failure(ex) =>
          p failure ApiInitException(ex)
          dom.console.error("FbAdapter: Fb.init() failed: " + ex.getClass.getName + ": " + ex.getMessage)
      } onComplete { case _ =>
        // Вычищаем эту функцию из памяти браузера.
        window.fbAsyncInit = null
      }
    }
    // Добавить скрипт facebook.js на страницу
    try {
      addScriptTag()
    } catch {
      case ex: Throwable =>
        p failure DomUpdateException(ex)
        dom.console.error("FbAdapter: addScriptTag() failed: " + ex.getClass.getName + ": " + ex.getMessage)
    }
    // Среагировать на слишком долгую загрузку скрипта таймаутом.
    val t = SCRIPT_LOAD_TIMEOUT_MS
    dom.setTimeout(
      {() =>
        if (!p.isCompleted) {
          p failure UrlLoadTimeoutException(SCRIPT_URL, t)
          dom.console.error("FbAdapter: timeout %s ms occured during ensureReady()", t)
        }
      },
      t
    )
    p.future
  }

  /**
   * Запуск логина и обрабока ошибок.
   * @return Фьючерс с выверенным результатом логина.
   */
  protected def doLogin(fbCtxOpt: Option[FbCtx])(implicit actx: IActionContext): Future[FbCtx] = {
    val allNeedPerms = FbPermissions.wantPublishPerms
    // TODO Нужно Fb.getAuthResponse подключить к работе. Но лучше вынести это всё в нормальный fsm и делать логин в ensureReady().
    val needPerms = fbCtxOpt.fold(allNeedPerms) { fbCtx =>
      // Вычитаем коллекции без использования Set
      allNeedPerms.filter { perm => !(fbCtx.hasPerms contains perm) }
    }
    if (needPerms.nonEmpty) {
      // Есть недостающие пермишшены. Нужно запросить login() с недостающими пермишшенами.
      val args = FbLoginArgs(
        scope = FbPermissions.permsToString( needPerms ),
        returnScopes = true
      )
      // Отправляем попап логина в очередь на экран.
      actx.app.popupQueue.enqueue { () =>
        Fb.login(args)

      }.flatMap { res =>
        // TODO Нужно вернуть новый fb-контекст с имеющимеся пермишшеннами из res.grantedPermissions
        res.authResp
          .filter { _ => res.status.isAppConnected }
          .map { authResp =>
            val grPerms = authResp.grantedScopes
            val fbctx1 = FbCtx(hasPerms = grPerms)
            Future successful fbctx1
          }
          .getOrElse {
            Future failed LoginCancelledException()
          }
      }

    } else {
      // Все необходимые пермишшены уже доступны прямо сейчас. Значит и юзер залогинен, и можно обойтись без логина.
      Future successful fbCtxOpt.get
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
  
  /** Определение типа целевого таргета. */
  protected def detectTgNodeType(target: MExtTarget): Future[FbTarget] = {
    // Первый шаг - определяем по узлу данные.
    val tgInfoOpt = FbTarget.fromUrl( target.tgUrl )
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
  }

  /** Подготовить данные по цели к публикации.
    * Запрашиваем специальный access_token, если требуется. */
  protected def mkTgInfo(fbTg: FbTarget, fbCtx: FbCtx): Future[FbTgPostingInfo] = {
    // TODO Прерывать процесс, если page, но нет ни manage_pages, ни publish_pages.
    if ( fbTg.nodeType.contains(FbNodeTypes.Page)  &&
         fbCtx.hasPerms.contains(FbPermissions.ManagePages) ) {
      // Для постинга на страницу нужно запросить page access token.
      getAccessToken(fbTg.nodeId)
        .map { pageAcTokOpt =>
        FbTgPostingInfo(nodeId = fbTg.nodeId, accessToken = pageAcTokOpt)
      }

    } else {
      // Нет возможности/надобности постить от имени страницы. Попытаться запостить от имени юзера.
      val res = FbTgPostingInfo(
        nodeId      = fbTg.nodeId,
        accessToken = Fb.getAuthResponse().flatMap(_.accessToken)
      )
      Future successful res
    }
  }

  /** Убедится, что размер картинки подходит под требования системы.
    * @param mctx0 Начальный контекст
    * @return Right() если не требуется изменять размер картинки.
    *         Left() с fillCtx-контекстом, который будет отправлен на сервер.
    */
  protected def ensureImgSz(fbTg: FbTarget, mctx0: MJsCtx): Either[MJsCtx, _] = {
    // TODO Сейчас проверяется только первая карточка
    val picSzOpt = mctx0.mads
      .headOption
      .flatMap(_.picture)
      .flatMap(_.sizeId)
      .flatMap(FbWallImgSizes.maybeWithName)
    val nt = fbTg.nodeType
    val fbWallImgSizeOpt = nt.map(_.wallImgSz)
    if (picSzOpt.isEmpty || fbWallImgSizeOpt.exists(_.szAlias == picSzOpt.get.szAlias) ) {
      dom.console.info("Current img size %s is OK for fb node type %s", picSzOpt.toString, nt.toString)
      // Сервер прислал инфу по картинке в правильном формате
      Right(None)

    } else {
      dom.console.info("Current img size %s is NOT OK for fb node type %s. Req to update img size to %s", picSzOpt.toString,
        nt.toString, fbWallImgSizeOpt.toString)
      // Формат картинки не подходит, но серверу доступен другой подходящий размер.
      Left(mctx0.copy(
        status = Some( MAnswerStatuses.FillContext ),
        custom = Some( FbCtx(fbTg = Some(fbTg)).toJson ),
        mads = mctx0.mads.map { mad =>
          // TODO Когда неск.карточек, то им нужно ведь разные размеры выставлять.
          mad.copy(
            picture = Some( MAdPictureCtx(
              sizeId = fbWallImgSizeOpt.map(_.szAlias)
            ))
          )
        }
      ))
    }
  }


  // TODO Нужно оформить нижеследующий код как полноценный FSM с next_state. Логин вызывать однократно в ensureReady() в blocking-режиме.

  /** Первый шаг, который заканчивается переходом на step2() либо fillCtx. */
  protected def step1(fbCtxOpt0: Option[FbCtx])(implicit actx: IActionContext): Future[MJsCtx] = {
    import actx.mctx0
    val fbCtx1Fut = doLogin(fbCtxOpt0)
    val fbTgFut = fbCtx1Fut flatMap { _ =>
      detectTgNodeType(mctx0.target.get)
    }
    fbCtx1Fut flatMap { fbCtx1 =>
      fbTgFut flatMap { fbTg =>
        ensureImgSz(fbTg, mctx0) match {
          case Left(mctx1) =>
            Future successful mctx1
          case _ =>
            val someTg = Some(fbTg)
            val fbCtx2 = fbCtx1.copy(fbTg = someTg)
            step2(fbCtx2)
        }
      }
    }
  }

  /** Второй шаг -- это шаг публикации. */
  protected def step2(fbCtx: FbCtx)(implicit actx: IActionContext): Future[MJsCtx] = {
    import actx.mctx0
    // Второй шаг: получение доп.данных по цели для публикации, если необходимо.
    mkTgInfo(fbCtx.fbTg.get, fbCtx) flatMap { tgInfo =>
      // Третий шаг - запуск публикации поста.
      publishPost(mctx0, tgInfo)

    } map { res =>
      // Если всё ок, вернут результат.
      mctx0.copy(
        status = Some(MAnswerStatuses.Success)
      )
    }
  }

  /** Запуск обработки одной цели. */
  override def handleTarget(implicit actx: IActionContext): Future[MJsCtx] = {
    import actx.mctx0
    val fbCtxOpt = mctx0.custom.map(FbCtx.fromJson)
    val hasTg = fbCtxOpt.exists(_.fbTg.isDefined)
    if (!hasTg) {
      step1(fbCtxOpt)
    } else {
      step2(fbCtxOpt.get)
    }
  }

}
