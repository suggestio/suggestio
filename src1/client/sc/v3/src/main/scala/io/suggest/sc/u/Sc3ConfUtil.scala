package io.suggest.sc.u

import io.suggest.common.empty.OptionUtil
import io.suggest.conf.ConfConst
import io.suggest.kv.MKvStorage
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.sc3.MSc3Init
import io.suggest.log.Log
import io.suggest.spa.StateInp
import play.api.libs.json.Json
import japgolly.univeq._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.06.18 18:34
  * Description: Конфигурация выдачи.
  */
object Sc3ConfUtil extends Log {

  /** Форсировать поведение */
  @inline def FORCE_PRODUCTION_MODE = true

  /** Некоторые шаги поведения выдачи определяются режимом компиляции, но это можно переопределять здесь. */
  @inline def isDevMode: Boolean =
    !FORCE_PRODUCTION_MODE && scalajs.LinkingInfo.developmentMode


  /** Сохранить конфигурацию выдачи в постоянное хранилище.
    *
    * @param init Инстанс конфигурации.
    */
  def saveInit(init: MSc3Init): Unit = {
    val mkv = MKvStorage(
      key   = ConfConst.SC_INIT_KEY,
      value = init,
    )
    MKvStorage.save( mkv )
  }

  /** Сохранение, если поддерживается платформой.
    * Метод никогда не возвращает ошибок.
    *
    * @return true - сохранено.
    *         false - ошибка где-то в логах, либо просто нет доступного хранилища.
    */
  def saveInitIfPossible(init: MSc3Init): Boolean = {
    try {
      MKvStorage.isAvailable && {
        saveInit(init)
        true
      }
    } catch {
      case ex: Throwable =>
        logger.error( ErrorMsgs.CONF_SAVE_FAILED, ex )
        false
    }
  }


  /** Собрать конфигурацию из возможных источников конфига.
    *
    * @return Опциональный конфиг.
    */
  def getSavedInit(): Option[MSc3Init] = {
    OptionUtil.maybeOpt( MKvStorage.isAvailable ) {
      for {
        confJson  <- MKvStorage.get[MSc3Init]( ConfConst.SC_INIT_KEY )
      } yield {
        confJson.value
      }
    }
  }


  /** Прочитать конфиг из DOM. */
  def getInitFromDom(): Option[MSc3Init] = {
    for {
      stateInput <- StateInp.find()
      jsonConfig <- stateInput.value
      confParsed <- {
        Try {
          Json
            .parse(jsonConfig)
            .validate[MSc3Init]
        }
          .toOption
      }
      conf <- {
        if (confParsed.isError)
          logger.error( ErrorMsgs.JSON_PARSE_ERROR, msg = confParsed )
        confParsed.asOpt
      }
    } yield {
      conf
    }
  }


  /** Получить конфиг из DOM или из сохранённого места, выбрав более свежий при конфликте.
    *
    * @return Опциональный MSc3Init.
    */
  def getFreshestInit(): Option[MSc3Init] = {
    // Конфиг может быть сохранён в постоянной хранилке, может быть ещё конфиг в DOM.
    // В любом случае надо отработать оба варианта.
    val inits = (
      getSavedInit() ::
      getInitFromDom() ::
      Nil
    )
      .flatten
    if (inits.isEmpty) {
      None

    } else if (inits.lengthCompare(1) ==* 0) {
      inits.headOption

    } else {
      // Есть несколько вариантов. Это значит один с сервера, а второй содержит какие-то обновления.
      // Надо выбрать наиболее свежий из имеющихся. Для этого надо сравнить conf.created/updated-значения.
      // Избегаем прямого сравнивания server и client-таймштампов, однако учитываем, что порядок timestamp'ов одинаковый.
      // Приоритет имеет инстанс с бОльшей serverCreated-датой, а при равных created - брать бОльший updated.
      val resultInit = inits.maxBy { init =>
        val c = init.conf
        // Защищаемся от возможных sjs opaque-type косяков через явный double:
        c.serverCreatedAt.toDouble * 31 + c.clientUpdatedAt.fold(0d)(_.toDouble)
      }
      Some(resultInit)
    }
  }

}

