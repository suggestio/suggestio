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
  def PARAMS = "params"

  /** Название agg-контекста внутри params. */
  def AGG =  "_agg"

  /** Название aggs-контекста внутри params. */
  def AGGS = AGG + "s"

  /** Название source-контекста внутри params. */
  def SOURCE = "_source"

}


/** Интерфейс для скриптов аггрегации. */
trait IAggScripts {

  def initScript: Script
  def mapScript: Script
  def combineScript: Script
  def reduceScript: Script

}
