package util.showcase

import io.suggest.util.logs.MacroLogsImpl
import japgolly.scalagraal._
import japgolly.scalagraal.js._
import GraalJs._
import GraalBoopickle._
import cats.effect.IO
import controllers.AssetsMetadata
import io.suggest.ctx.CtxData
import io.suggest.routes.RoutesJvmConst
import io.suggest.sc.ssr.{MScSsrArgs, ScSsrProto, SsrLangData}
import io.suggest.sjs.SjsUtil
import io.suggest.ueq.UnivEqUtilJvm._
import japgolly.scalagraal.ScalaGraalEffect.AsyncES
import play.api.{Application, Mode}
import japgolly.univeq._
import models.mctx.ContextFactory
import models.req.{IReqHdr, MSioUsers}
import org.graalvm.polyglot.{Context, PolyglotAccess, Source}
import play.api.http.{HeaderNames, HttpVerbs}
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.{ApplicationLifecycle, Injector}
import play.api.libs.typedmap.TypedMap
import play.api.mvc.Headers
import play.api.mvc.request.{RemoteConnection, RequestTarget}
import views.js.sc.ScJsRouterTpl
import _root_.util.i18n.JsMessagesUtil
import io.suggest.i18n.{I18nConst, MLanguage, MLanguages}

import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton // singleton, because pool is here.
final class ScSsrUtil @Inject()(
                                 injector: Injector,
                               )
  extends MacroLogsImpl
{

  private lazy val current = injector.instanceOf[Application]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  private def contextFactory = injector.instanceOf[ContextFactory]
  private def mSioUsers = injector.instanceOf[MSioUsers]
  private def messagesApi = injector.instanceOf[MessagesApi]
  private def scJsRouterTpl = injector.instanceOf[ScJsRouterTpl]
  private lazy val jsMessagesUtil = injector.instanceOf[JsMessagesUtil]

  def POOL_SIZE = current.configuration.getOptional[Int]("sc.ssr.js.pool.size").getOrElse(1)
  def JS_DEBUG = current.configuration.getOptional[Boolean]("sc.ssr.js.inspect.enabled").getOrElse(false)
  def JS_DEBUG_PORT_START = current.configuration.getOptional[Int]("sc.ssr.js.inspect.port.from").getOrElse(1)
  def JS_DEBUG_URL_HOST = "localhost"

  /** Create new thread pool for current usage. */
  private lazy val _graalCtxPool: GraalContextPool[Future] = {
    // https://github.com/oracle/graaljs/issues/495
    Thread.currentThread().setContextClassLoader( classOf[Context].getClassLoader )

    lazy val _setupExpr = graalSetupExpr
    val jsLang = implicitly[Language]

    lazy val freePortNumber = new AtomicInteger( JS_DEBUG_PORT_START )
    lazy val jsDebugPath = java.util.UUID.randomUUID().toString

    val r = GraalContextPool
      .Builder
      .fixedThreadPool( POOL_SIZE )
      .fixedContextPerThread { engine =>
        val cxBuilder = Context
          .newBuilder( jsLang.name )
          // Copypaste from scalagraal/InternalsUtils.scala:
          // So Polyglot.import works:
          .allowPolyglotAccess( PolyglotAccess.ALL )
          // So methods on Java objects can be invoked, Java arrays are recognised as JS arrays, etc:
          .allowAllAccess( true )

        if (JS_DEBUG) {
          // We need chrome debugger in dev mode: https://www.graalvm.org/tools/chrome-debugger/#programmatic-launch-of-inspector-backend
          val portNumber = freePortNumber.getAndIncrement().toString
          cxBuilder
            .option( "inspect", portNumber )
            .option( "inspect.Path", jsDebugPath )

          val devToolsUrl = String.format( "devtools://devtools/bundled/js_app.html?ws=%s:%s/%s", JS_DEBUG_URL_HOST, portNumber, jsDebugPath )
          LOGGER.info(s"(<!>) GraalVM JSContext DEBUG PORT:$portNumber $devToolsUrl")
        } else {
          cxBuilder
            .engine( engine )
        }

        cxBuilder
          .build()
      }
      // Move async effects into cats-effect:
      /*.resultType[IO]( new AsyncES[IO] {
        override def apply(es: ExecutorService): ScalaGraalEffect.Async[IO] = {
          new ScalaGraalEffect.Async[IO] {
            override def delay[A](a: => A) = IO.delay( a )
          }
        }
      })*/
      .configure { gCtxB =>
        gCtxB.onContextCreate( _setupExpr )
      }
      .build()


    // Rollback class-loader...
    Thread.currentThread().setContextClassLoader( classOf[ScSsrUtil].getClassLoader )

    // Purge pool contexts on shutdown:
    injector
      .instanceOf[ApplicationLifecycle]
      .addStopHook { () =>
        Future {
          _graalCtxPool.unsafeShutdown()
        }
      }

    r
  }



  /** Globally-exported rendering function. */
  def _renderActionSyncFn = Expr
    .fn1[MScSsrArgs]( ScSsrProto.Manifest.RenderActionSync )
    .compile(_.asString)



  /** List of scala.js script file names. */
  def sjsScripts = {
    val r = SjsUtil.jsScripts(
      fileNamePrefix = "showcasessr",          // TODO Deduplicate costant with /build.sbt -> val showcaseSsrJs.withId(args)
      playIsProd = current.mode ==* Mode.Prod,      // TODO Need to check sbt scalaJSStage ? FullOptStage | FastOptStage
    )
    LOGGER.trace(s"sjsScripts(): => ${r.mkString(" | ")}")
    r
  }

  /** Mock templates rendering context. */
  def mockTemplatesContext( i18nLang: MLanguage ): models.mctx.Context = {
    contextFactory.create(
      request = new IReqHdr {
        override def user = mSioUsers.empty
        override def method = HttpVerbs.GET
        override def version: String = "1.1"
        override def target = RequestTarget( "/", "/", Map.empty )
        override def connection = RemoteConnection( "127.0.0.1", secure = false, clientCertificateChain = None )
        override def headers = Headers(
          HeaderNames.HOST -> "suggest.io",
        )
        override def attrs = TypedMap.empty
      },
      messages = messagesApi.preferred( Lang( i18nLang.value ) :: Nil ),
      ctxData = CtxData.empty,
    )
  }


  /** Render jsRoutes.js content. */
  def jsRouterJs()(implicit ctx: models.mctx.Context): String =
    scJsRouterTpl().body


  def jsMessagesJs()(implicit ctx: models.mctx.Context): String = {
    jsMessagesUtil
      .sc( Some(I18nConst.JSMESSAGES_NAME) )(ctx.messages)
      .body
  }

  /** Initial expressions for newly created GraalContext. */
  private def graalSetupExpr: Expr[_] = {
    // We sure, classLoader already configured upper level.
    val relPathPrefix = RoutesJvmConst.ASSETS_PUBLIC_ROOT
    val langName = implicitly[Language].name
    implicit val ctx = mockTemplatesContext( MLanguages.default )

    // Render default english jsMessages.
    val jsMessagesExpr = Expr {
      val path = "messages-en.js"
      LOGGER.trace(s"graalSetupExpr: + $path")
      Source
        .newBuilder( langName, jsMessagesJs(), path )
        .build()
    }

    /** Render js-routes... */
    val jsRouterExpr = Expr {
      val path = "jsRouter.js"
      LOGGER.trace(s"graalSetupExpr: + $path")
      Source
        .newBuilder( langName, jsRouterJs(), path )
        .build()
    }

    val sjsExprs = sjsScripts
      .iterator
      .map { jsFileName =>
        Expr {
          val path = relPathPrefix + "/" + jsFileName
          LOGGER.trace(s"graalSetupExpr: + $jsFileName")

          (if (path contains "-loader.") {
            // var global {} -- need for `raf` npm.js package. https://github.com/chrisdickinson/raf/issues/46
            val jsCode = """
              |var exports = globalThis;
              |exports.require = ScalaJSBundlerLibrary.require;
              |var require = ScalaJSBundlerLibrary.require;
              |var global = globalThis;
              |""".stripMargin

            Source.newBuilder( langName, jsCode, path )
          } else {
            val url = current.environment.resource( path )
            LOGGER.trace(s"graalSetupExpr: $path passed as file URL: ${url.orNull})")
            Source.newBuilder( langName, url.get )
          })
            .build()
        }
      }
      .reduce(_ >> _)

    // Currently, ReactSsr.Setup adds global window{}. This fails react-stonecutter because https://github.com/dantrain/react-stonecutter/issues/7
    // TODO When react-stonecutter will be replaced with something else (mui/Masonry?), these exprs should be re-done according to documentation.
    //      ReactSsr.Setup( exprs.dropRight(1): _* ) >> exprs.last

    jsRouterExpr >> jsMessagesExpr >> sjsExprs
  }


  /** Detect showcase supported language enum.element from request. */
  def ssrLang()(implicit scSiteCtx: models.mctx.Context): Some[MLanguage] =
    Some( MLanguages.byCode( scSiteCtx.messages.lang.code ) )


  /** Prepare language data container before call. */
  def ssrLangDataFromRequest(langFromRequest: MLanguage)(implicit scSiteCtx: models.mctx.Context): Some[SsrLangData] = {
    val ssrLangData = SsrLangData(
      lang        = langFromRequest,
      messagesMap = jsMessagesUtil.sc.messages( scSiteCtx.messages )
    )
    Some( ssrLangData )
  }


  /** Showcase SSR rendering routine.
    *
    * @param args Showcase SSR args container.
    * @return HTML-rendered content (index+grid+etc).
    */
  def renderShowcaseContent(args: MScSsrArgs): Future[Expr.Result[String]] = {
     // IO.defer
    val pool = _graalCtxPool
    pool.eval( _renderActionSyncFn( args ) )
      //.andThen { case _ => pool.unsafeShutdown() }
  }

}
