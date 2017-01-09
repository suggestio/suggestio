package io.suggest.util

/** Трейты поддержки логгирования через макросы. */

import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.{Logger => MacroLogger}

object MacroLogsImpl extends Serializable {
  def getMacroLogger(clazz: Class[_]) = MacroLogger(LoggerFactory.getLogger(clazz))
}

import MacroLogsImpl._


/** Интерфейс поля логгера. */
trait MacroLogsI {
  def LOGGER: MacroLogger
}

trait MacroLogsDyn extends MacroLogsI {
  override def LOGGER = getMacroLogger(getClass)
}

/** Используем scala macros логгирование, которое НЕ порождает вообще лишнего мусора и куч анонимных функций.
  * Трейт подмешивается в класс, и затем нужно сделать "import LOGGER._". Это импортнёт scala-макросы как методы. */
trait MacroLogsImpl extends MacroLogsDyn {
  @transient
  override val LOGGER = super.LOGGER
}
trait MacroLogsImplLazy extends MacroLogsDyn {
  @transient
  override lazy val LOGGER = super.LOGGER
}



/** Доп-функции для [[io.suggest.util.MacroLogsImpl]], который добавляется короткие вызовы для isXXXXEnabled(). */
trait MacroLogsUtil extends MacroLogsI {
  def isTraceEnabled = LOGGER.underlying.isTraceEnabled
  def isDebugEnabled = LOGGER.underlying.isDebugEnabled
  def isInfoEnabled  = LOGGER.underlying.isInfoEnabled
  def isWarnEnabled  = LOGGER.underlying.isWarnEnabled
  def isErrorEnabled = LOGGER.underlying.isErrorEnabled
}


