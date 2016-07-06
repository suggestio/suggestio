package io.suggest.sc.sjs.m.msc

import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.util.SjsLogger

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.07.16 10:38
  * Description: Модель/статическая утиль для random seed (generation).
  */
object MGen extends SjsLogger {

  /** Распарсить сериализованный вариант. */
  def parse(genStr: String): Option[Long] = {
    try {
      Some(genStr.toLong)
    } catch {
      case ex: Throwable =>
        warn( WarnMsgs.GEN_NUMBER_PARSE_ERROR, ex )
        None
    }
  }

  /** Сериализовать значение generation. */
  def serialize(gen: Long): Any = {
    gen
  }

  /** Сгенерить новое значение generation. */
  def random: Long = {
    (js.Math.random() * 1000000000).toLong
  }

}
