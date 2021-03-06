package io.suggest.util.logs

/** Трейты поддержки логгирования через макросы. */

import com.typesafe.scalalogging.{Logger => MacroLogger}

object MacroLogs extends Serializable {

  def getMacroLogger(clazz: Class[_]) = MacroLogger( clazz )

}


/** Интерфейс поля логгера. */
trait IMacroLogs {
  def LOGGER: MacroLogger
}

trait MacroLogsDyn extends IMacroLogs {
  override def LOGGER = MacroLogs.getMacroLogger(getClass)
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


/** Доп-функции для [[MacroLogsImpl]], который добавляется короткие вызовы для isXXXXEnabled(). */
trait MacroLogsUtil extends IMacroLogs {
  def isTraceEnabled = LOGGER.underlying.isTraceEnabled
  def isDebugEnabled = LOGGER.underlying.isDebugEnabled
  def isInfoEnabled  = LOGGER.underlying.isInfoEnabled
  def isWarnEnabled  = LOGGER.underlying.isWarnEnabled
  def isErrorEnabled = LOGGER.underlying.isErrorEnabled
}


