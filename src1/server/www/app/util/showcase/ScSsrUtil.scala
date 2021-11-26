package util.showcase

import io.suggest.util.logs.MacroLogsImpl
import japgolly.scalagraal._
import japgolly.scalagraal.js._
import GraalJs._
import GraalBoopickle._
import cats.effect.IO
import controllers.AssetsMetadata
import io.suggest.routes.RoutesJvmConst
import io.suggest.sc.ssr.{MScSsrArgs, ScSsrProto}
import io.suggest.sjs.SjsUtil
import io.suggest.ueq.UnivEqUtilJvm._
import japgolly.scalagraal.ScalaGraalEffect.AsyncES
import play.api.{Environment, Mode}
import japgolly.univeq._
import org.graalvm.polyglot.{Context, Source}
import play.api.inject.{ApplicationLifecycle, Injector}

import java.io.File
import java.util.concurrent.ExecutorService
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton // singleton, because pool is here.
final class ScSsrUtil @Inject()(
                                 injector: Injector,
                               )
  extends MacroLogsImpl
{

  private lazy val env = injector.instanceOf[Environment]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /** Create new thread pool for current usage. */
  private lazy val _graalCtxPool: GraalContextPool[Future] = {
    // https://github.com/oracle/graaljs/issues/495
    Thread.currentThread().setContextClassLoader( classOf[Context].getClassLoader )

    lazy val _setupExpr = graalSetupExpr

    val r = GraalContextPool
      .Builder
      .fixedThreadPool( 1 )
      .fixedContextPerThread()
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
      playIsProd = env.mode ==* Mode.Prod,      // TODO Need to check sbt scalaJSStage ? FullOptStage | FastOptStage
    )
    LOGGER.trace(s"sjsScripts(): => ${r.mkString(" | ")}")
    r
  }


  /** Initial expressions for newly created GraalContext. */
  private def graalSetupExpr: Expr[_] = {
    // We sure, classLoader already configured upper level.
    val relPathPrefix = RoutesJvmConst.ASSETS_PUBLIC_ROOT
    sjsScripts
      .iterator
      .map { jsFileName =>
        Expr {
          val path = relPathPrefix + "/" + jsFileName
          LOGGER.trace(s"graalSetupExpr: + $jsFileName")
          val langName = implicitly[Language].name

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
            val url = env.resource( path )
            LOGGER.trace(s"graalSetupExpr: + $path => ${url.orNull})")
            Source.newBuilder( langName, url.get )
          })
            .build()
        }
      }
      .reduce(_ >> _)
    // Currently, ReactSsr.Setup adds global window{}. This fails react-stonecutter because https://github.com/dantrain/react-stonecutter/issues/7
    // TODO When react-stonecutter will be replaced with something else (mui/Masonry?), these exprs should be re-done according to documentation.
    //      ReactSsr.Setup( exprs.dropRight(1): _* ) >> exprs.last
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
