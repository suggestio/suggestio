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
  protected def headlessInit(implicit actx: IActionContext): Future[FbJsCtxT] = {
    // Запускаем инициализацию FB API.
    val appId = actx.mctx0.service
      .flatMap(_.appId)
      .orNull
    val opts = FbInitOptions(appId)
    val initFut = Fb.init(opts)

    // Сразу запрашиваем инфу по fb-залогиненности текущего юзера и права доступа.
    val loginStatusEarlyFut = initFut flatMap { _ =>
      Fb.getLoginStatus()
    }

    // Если юзер не подключил приложение или не залогинен в facebook, то надо вызывать окно FB-логина без каких-либо конкретных прав.
    val loginStatusFut = loginStatusEarlyFut
      .filter { _.status.isAppConnected }
      .recoverWith { case ex: Throwable =>
        // Отправляем окно FB-логина в очередь на отображение.
        val res = actx.app.popupQueue.enqueue { () =>
          val args = FbLoginArgs(
            returnScopes  = Some(true)
          )
          Fb.login(args)
        }
        if (!ex.isInstanceOf[NoSuchElementException])
          dom.console.warn("FB failed to getLoginStatus(): %s: %s", ex.getClass.getName, ex.getMessage)
        res
      }
      .filter { _.status.isAppConnected }
      .recoverWith { case ex: Throwable =>
        if (!ex.isInstanceOf[NoSuchElementException])
          dom.console.warn("FbAdapter.headlessInit(): Failed to request fb permissions: %s: %s", ex.getClass.getName, ex.getMessage)
        Future failed LoginCancelledException()
      }

    // Когда появится инфа о текущем залогиненном юзере, то нужно запросить уже доступные пермишшены.
    val earlyHasPermsFut = loginStatusFut
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

    val hasPermsFut = earlyHasPermsFut
      .recoverWith { case ex: NoSuchElementException =>
        loginStatusFut
          .map { _.authResp.get.grantedScopes }
      }

    // Параллельно вычисляем данные по целям.
    val newTargetsFut = loginStatusFut.flatMap { _ =>
      Future.traverse( actx.mctx0.svcTargets ) { tg =>
        detectTgNodeType(tg)
          .map { FbExtTarget(tg, _) }
      }
    }
    // Вычисляем необходимые для целей пермишшены на основе имеющегося списка целей.
    val tgsWantPermsFut = newTargetsFut map { tgts =>
      tgts.iterator
        .flatMap { _.fbTgUnderlying.nodeType }
        .flatMap { _.publishPerms }
        .toSet
    }

    // Вычисляем недостающие пермишшены, которые нужно запросить у юзера.
    val needPermsFut = for {
      hasPerms      <- hasPermsFut
      tgsWantPerms  <- tgsWantPermsFut
    } yield {
      val res = tgsWantPerms -- hasPerms
      println("I have perms = " + hasPerms.mkString(",") + " ; tg want perms = " + tgsWantPerms.mkString(",") + " ; so, need perms = " + res.mkString(","))
      res
    }

    // Отправляем попап логина в очередь на экран для получения новых пермишеннов, если такое требуется.
    val hasPerms2Fut = needPermsFut
      .filter { _.nonEmpty }
      .flatMap { needPerms =>
        println("Asking for new FB permissions: " + needPerms.mkString(", "))
        actx.app.popupQueue.enqueue { () =>
          val args = FbLoginArgs(
            scopes        = needPerms,
            returnScopes  = Some(true),
            authType      = Some(FbAuthTypes.ReRequest)
          )
          Fb.login(args)
            .map { _.authResp.fold [Seq[FbPermission]] (Nil) (_.grantedScopes) }
        }
      }
      .recoverWith {
        case ex: NoSuchElementException =>
          println("Permissing request is not needed. Skipping.")
          hasPermsFut
      }

    // Собираем fb-контекст.
    val fbCtxFut = hasPerms2Fut map { hasPerms =>
      FbCtx(hasPerms = hasPerms)
    }

    // Формируем и возвращаем результирующий внутренний контекст.
    for {
      _fbCtx  <- fbCtxFut
      tgts    <- newTargetsFut
    } yield {
      new FbJsCtxT {
        override def jsCtxUnderlying  = actx.mctx0
        override def fbCtx            = _fbCtx
        override def status           = Some(MAnswerStatuses.Success)
        override def svcTargets       = tgts
      }
    }
  }


  /** Запуск инициализации клиента. Добавляется необходимый js на страницу,  */
  override def ensureReady(implicit actx: IActionContext): Future[MJsCtxT] = {
    // В этот promise будет закинут результат.
    val p = Promise[MJsCtxT]()
    // Чтобы зафиксировать таймаут загрузки скрипта fb, используется второй promise:
    val scriptLoadP = Promise[Null]()
    // Подписаться на событие загрузки скрипта.
    val window: FbWindow = dom.window
    window.fbAsyncInit = { () =>
      // FB-скрипт загружен. Сообщаем об этом контейнеру scriptLoadPromise.
      scriptLoadP success null
      // Запускаем инициализацию.
      val initFut = headlessInit
        // Любое исключение завернуть в ApiInitException
        .recoverWith { case ex: Throwable =>
          dom.console.error("FbAdapter: Fb.init() failed: " + ex.getClass.getName + ": " + ex.getMessage)
          Future failed ApiInitException(ex)
        }
      p completeWith initFut
      // Вычищаем эту функцию из памяти браузера, когда она подходит к концу.
      window.fbAsyncInit = null
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
        if (!scriptLoadP.isCompleted) {
          p failure UrlLoadTimeoutException(SCRIPT_URL, t)
          dom.console.error("FbAdapter: timeout %s ms occured during ensureReady()", t)
        }
      },
      t
    )
    p.future
  }


  /** Сборка и публикация поста. */
  protected def publishPost(mctx0: FbJsCtx, tgInfo: IFbPostingInfo): Future[_] = {
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
    Fb.getNodeInfo(args) map { resp =>
      // Если нет токена в ответе, то скорее всего там указана ошибка. Её рендерим в логи для отладки.
      if (resp.error.isDefined)
        dom.console.warn("Failed to acquire access token: %s => error %s", args.toString, resp.error.toString)
      resp.accessToken
    }
  }
  
  /** Определение типа целевого таргета. */
  protected def detectTgNodeType(target: IMExtTarget): Future[FbTarget] = {
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
  protected def mkTgInfo(fbTg: IFbTarget, fbCtx: FbCtx): Future[FbTgPostingInfo] = {
    // TODO Прерывать процесс, если page, но нет ни manage_pages, ни publish_pages?
    val accTokOptFut: Future[Option[String]] = {
      if ( fbTg.nodeType.contains(FbNodeTypes.Page)  &&  fbCtx.hasPerms.contains(FbPermissions.ManagePages) ) {
        // Для постинга на страницу нужно запросить page access token.
        getAccessToken(fbTg.nodeId)

      } else {
        // Нет возможности/надобности постить от имени страницы. Попытаться запостить от имени юзера.
        val acTokOpt = Fb.getAuthResponse().flatMap(_.accessToken)
        Future successful acTokOpt
      }
    }
    accTokOptFut map { tokOpt =>
      FbTgPostingInfo(
        nodeId      = fbTg.nodeId,
        accessToken = tokOpt
      )
    }
  }

  /** Убедится, что размер картинки подходит под требования системы.
    * @param mctx0 Начальный контекст
    * @return Right() если не требуется изменять размер картинки.
    *         Left() с fillCtx-контекстом, который будет отправлен на сервер.
    */
  protected def ensureImgSz(fbTg: IFbTarget, mctx0: FbJsCtx): Either[MJsCtxT, _] = {
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
      // Формат картинки не подходит, но серверу доступен другой подходящий размер.
      dom.console.info("Current img size %s is NOT OK for fb node type %s. Req to update img size to %s", picSzOpt.toString,
        nt.toString, fbWallImgSizeOpt.toString)
      // Генерим результирующий контекст на основе текущего контекста.
      val res = new FbJsCtxWrapperT {
        override val jsCtxUnderlying = mctx0
        override val fbCtx: FbCtx = {
          mctx0.fbCtx.copy(
            step = Some(2)
          )
        }
        override val status = Some( MAnswerStatuses.FillContext )
        override val mads: Seq[MAdCtx] = {
          mctx0.mads.map { mad =>
            // TODO Когда неск.карточек, то им нужно ведь разные размеры выставлять.
            mad.copy(
              picture = Some( MAdPictureCtx(
                sizeId = fbWallImgSizeOpt.map(_.szAlias)
              ))
            )
          }
        }
      }
      Left(res)
    }
  }


  // TODO Нужно оформить нижеследующий код как полноценный FSM с next_state. Логин вызывать однократно в ensureReady() в blocking-режиме.

  /** Первый шаг, который заканчивается переходом на step2() либо fillCtx. */
  protected def step1(mctx0: FbJsCtx)(implicit actx: IActionContext): Future[MJsCtxT] = {
    val fbTg = mctx0.target.get
    ensureImgSz(fbTg, mctx0) match {
      case Left(mctx1) =>
        Future successful mctx1
      case _ =>
        step2(mctx0)
    }
  }

  /** Второй шаг -- это шаг публикации. */
  protected def step2(mctx0: FbJsCtx)(implicit actx: IActionContext): Future[MJsCtxT] = {
    val fbCtx = mctx0.fbCtx
    // Второй шаг: получение доп.данных по цели для публикации, если необходимо.
    mkTgInfo(mctx0.target.get, fbCtx) flatMap { tgInfo =>
      // Третий шаг - запуск публикации поста.
      publishPost(mctx0, tgInfo)

    } map { res =>
      // Если всё ок, вернут результат.
      mctx0.jsCtxUnderlying.copy(
        status = Some(MAnswerStatuses.Success)
      )
    }
  }

  /** Запуск обработки одной цели. */
  override def handleTarget(implicit actx: IActionContext): Future[MJsCtxT] = {
    val mctx0 = FbJsCtx(actx.mctx0)
    // fb-context был выставлен в ensureReady()
    mctx0.fbCtx.step match {
      case Some(2) =>
        step2(mctx0)
      case _ =>
        step1(mctx0)
    }
  }

}
