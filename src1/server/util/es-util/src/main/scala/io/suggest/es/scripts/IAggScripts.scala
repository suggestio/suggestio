package io.suggest.es.scripts

import org.elasticsearch.script.Script

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.18 22:09
  * Description: Интерфейс для сборки скриптов аггрегации.
  */
object IAggScripts {

  /** Константы скриптов аггрегации. */
  final def PARAMS = "params"

  final def STATE = "state"
  final def STATES = STATE + "s"

  /** Название source-контекста внутри params. */
  final def SOURCE = "_source"

}


/** Интерфейс для скриптов аггрегации. */
trait IAggScripts {

  def initScript: Script
  def mapScript: Script
  def combineScript: Script
  def reduceScript: Script

}
