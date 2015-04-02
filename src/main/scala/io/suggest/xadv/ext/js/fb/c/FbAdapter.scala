package io.suggest.xadv.ext.js.fb.c

import io.suggest.xadv.ext.js.fb.c.hi.Fb
import io.suggest.xadv.ext.js.fb.m._
import io.suggest.xadv.ext.js.runner.c.IActionContext
import io.suggest.xadv.ext.js.runner.c.adp.AsyncInitAdp
import io.suggest.xadv.ext.js.runner.m.ex._
import io.suggest.xadv.ext.js.runner.m._
import org.scalajs.dom
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 17:24
 * Description: Адаптер для постинга в facebook.
 */

class FbAdapter extends AsyncInitAdp {

  override type Ctx_t = FbJsCtxT

  override def SCRIPT_URL = "https://connect.facebook.net/en_US/sdk.js"
  override def SCRIPT_LOAD_TIMEOUT_MS = 10000

  /** Относится ли указанный домен к текущему клиенту? */
  override def isMyDomain(domain: String): Boolean = {
    domain matches "(www\\.)?facebook.(com|net)"
  }

  override def setInitHandler(handler: () => _): Unit = {
    val wnd: FbWindow = dom.window
    wnd.fbAsyncInit = handler
  }

  /** Эта инициализация вызывается, когда скрипт загружен. */
  override def serviceScriptLoaded(implicit actx: IActionContext): Future[Ctx_t] = {
    new FbInit().main()
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
