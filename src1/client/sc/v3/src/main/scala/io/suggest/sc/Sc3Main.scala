package io.suggest.sc

import io.suggest.event.WndEvents
import io.suggest.common.html.HtmlConstants
import io.suggest.cordova.CordovaConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.log.buffered.BufLogAppender
import io.suggest.log.filter.SevereFilter
import io.suggest.log.remote.RemoteLogAppender
import io.suggest.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.proto.http.HttpConst
import io.suggest.pwa.WebAppUtil
import io.suggest.sc.m.ScreenResetPrepare
import io.suggest.sc.router.SrvRouter
import io.suggest.log.{Log, LogSeverities, Logging}
import io.suggest.sc.v.styl.ScCssStatic
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.vm.spa.LkPreLoader
import io.suggest.sjs.leaflet.{Leaflet, LeafletGlobal}
import io.suggest.spa.StateInp
import io.suggest.sw.SwOptions
import org.scalajs.dom.experimental.serviceworkers._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLInputElement
import io.suggest.sjs.common.vm.evtg.EventTargetVm._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 11:42
  * Description: Запускалка выдачи третьего поколения.
  */
object Sc3Main extends Log {

  implicit private class TryOps[T]( val tryRes: Try[T] ) extends AnyVal {

    /** Короткий код для логгирования сообщений. */
    def logFailure( errMsg: ErrorMsg_t, msg: Any = null ): Try[T] = {
      for (ex <- tryRes.failed) {
        // Безопасно тут вызывать Log прямо во время инициализации, в т.ч. логгера?
        logger.error( errMsg + HtmlConstants.SPACE + ex + HtmlConstants.SPACE + msg )
        // Вернуть экзепшен дальше в dev-режиме?
        if (scalajs.LinkingInfo.developmentMode)
          throw ex
      }

      tryRes
    }

  }


  /** Здесь начинается исполнение кода выдачи. */
  def main(args: Array[String]): Unit = {
    if (JsApiUtil.isDefinedSafe( LeafletGlobal.L ))
      Try( Leaflet.noConflict() )

    val modules = new Sc3Module
    if (scalajs.LinkingInfo.developmentMode)
      Sc3Module.ref = modules

    Try {
      if (CordovaConstants.isCordovaPlatform())
        modules.sc3LeafletOverrides.mapPatch()
    }
      .logFailure( ErrorMsgs.NATIVE_API_ERROR )


    // Активировать отправку логов на сервер:
    Try {
      Logging.LOGGERS ::= new SevereFilter(
        minSeverity = LogSeverities.Warn,
        underlying = new BufLogAppender(
          underlying = new RemoteLogAppender(
            httpConfig = modules.ScHttpConf.mkRootHttpClientConfigF,
          ),
        ),
      )
    }
      .logFailure( ErrorMsgs.LOG_APPENDER_FAIL )

    // Сразу поискать js-роутер на странице.
    SrvRouter.ensureJsRouter()

    val doc  = DocumentVm()
    val docLoc = doc._underlying.location

    // 2018-02-27: После установки веб-приложения, есть проблема, что запуск приложения идёт по уже установленным
    // координатам из исходного URL. На новых девайсах это решабельно через webmanifest.start_url, а на яббле нужен доп.костыль:
    Try {
      if (
        WebAppUtil.isStandalone() &&
        Option(docLoc.hash).exists(_.nonEmpty)
      ) {
        docLoc.hash = ""
      }
    }
      .logFailure( ErrorMsgs.SHOULD_NEVER_HAPPEN )

    // Запустить фоновую установку ServiceWorker'а:
    Try {
      def sw = dom.window.navigator.serviceWorker
      for {
        // Найти на странице input ссылки скрипта воркера.
        inputEl <- Option( doc._underlying.getElementById(ScConstants.Sw.URL_INPUT_ID) )
        if JsApiUtil.isDefinedSafe( sw )
        // Не переустанавливать уже установленный sw. TODO получать версию SW с сервера в хидерах ответов, и перерегистрировать.
        swInp = StateInp( inputEl.asInstanceOf[HTMLInputElement] )
        swUrl2 <- swInp.value
        // Проверить URL service-worker'а, надо ли устанавливать/обновлять sw.js.
        if Try {
          val swCtl = sw.controller
          swCtl == null || {
            val swUrl0 = swCtl.scriptURL
            //println("swUrl0 = " + swUrl0 + "\nswUrl2 = " + swUrl2)
            (swUrl0 == null) ||
            !(swUrl0 endsWith swUrl2) ||
            // Для http-протокола разрешить установку всегда, т.к. это локалхост в dev-режиме выдачи. В prod-режиме может быть только https
            (docLoc.protocol equalsIgnoreCase HttpConst.Proto.HTTP_)
          }
        }.getOrElse(true)
      } {
        //println("installing sw")
        val swOpts = new SwOptions {
          override val scope = HtmlConstants.`.`
        }
        // Установка service-worker:
        for {
          ex <- sw
            .register( swUrl2, swOpts )
            .toFuture
            .failed
        } {
          println("!sw " + ex)
        }
      }
    }
      .logFailure( "!!SW" )

    val body = doc.body

    // Самый корневой рендер -- отрабатывается первым.
    val rootDiv = {
      val id = ScConstants.Layout.ROOT_ID
      Try {
        doc._underlying
          .getElementById( id )
      }
        .getOrElse {
          val div = VUtil.newDiv()
          div.setAttribute( "id", id )
          body.appendChild( div )
          div
        }
    }

    // Отрендерить компонент spa-роутера в целевой контейнер.
    for {
      ex <- Try {
        modules.sc3SpaRouter
          .state
          .router()
          .renderIntoDOM( rootDiv )
      }
        .failed
    } {
      logger.error( ErrorMsgs.INIT_FLOW_UNEXPECTED, ex )
      dom.window.alert( MCommonReactCtx.default.messages( MsgCodes.`Unsupported.browser.or.fatal.failure` ) + "\n\n" + ex.toString )
    }


    Try {
      body.className += ScCssStatic.Body.ovh.htmlClass //+ HtmlConstants.SPACE + BodyCss.BgLogo.ru.htmlClass
    }
      .logFailure( ErrorMsgs.SHOULD_NEVER_HAPPEN, body )

    // Инициализировать LkPreLoader:
    Try {
      for {
        lkPreLoader <- LkPreLoader.find()
      } yield {
        // TODO Opt дважды вызывается find() тут.
        LkPreLoader.PRELOADER_IMG_URL
        lkPreLoader.remove()
      }
    }
      .logFailure( ErrorMsgs.IMG_EXPECTED, LkPreLoader )

    // Подписать circuit на глобальные события window:
    Try {
      for {
        evtName <- WndEvents.RESIZE :: WndEvents.ORIENTATION_CHANGE :: Nil
      } {
        Try {
          dom.window.addEventListener4s( evtName ) { _: Event =>
            modules.sc3Circuit.dispatch( ScreenResetPrepare )
          }
        }
          .logFailure( ErrorMsgs.EVENT_LISTENER_SUBSCRIBE_ERROR, evtName )
      }
    }
      .logFailure( ErrorMsgs.EVENT_LISTENER_SUBSCRIBE_ERROR )

    // Подписаться на обновление заголовка и обновлять заголовок.
    // Т.к. document.head.title -- это голая строка, то делаем рендер строки прямо здесь.
    Try {
      modules.sc3Circuit.subscribe( modules.sc3Circuit.titlePartsRO ) { titlePartsRO =>
        dom.document.title = {
          (titlePartsRO.value.iterator ++ Iterator.single( MsgCodes.`Suggest.io` ))
            .mkString( " | " )
        }
      }
    }
      .logFailure( ErrorMsgs.UNEXPECTED_EMPTY_DOCUMENT )

  }

}
