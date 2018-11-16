package io.suggest.es.scripts

import org.elasticsearch.script.Script

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.18 22:18
  * Description: Наборы скриптов для дистанционных рассчётов контрольных сумм документов.
  */
object DocHashSumsAggScripts {
  def HASHES = "hashes"
}

/** Общий код скриптов, которые занимаются сборкой хэшей от документов. */
trait DocHashSumsAggScripts extends IAggScripts {

  import IAggScripts._
  import DocHashSumsAggScripts._

  override def initScript: Script = {
    new Script(s"$PARAMS.$AGG.$HASHES = []")
  }

  override def combineScript: Script = {
    new Script(s"int xsum = 0; for (h in $PARAMS.$AGG.$HASHES) { xsum += h } return xsum")
  }

  override def reduceScript: Script = {
    new Script(s"int asum = 0; for (a in $PARAMS.$AGGS) { asum += a } return asum")
  }

}

/** Скрипты для сборки хешей по перечисленным полям. */
case class FieldsHashSumsAggScripts(
                                     sourceFields: Iterable[String]
                                   )
  extends DocHashSumsAggScripts
{

  import IAggScripts._
  import DocHashSumsAggScripts._

  if (sourceFields.isEmpty)
    throw new IllegalArgumentException("sourceFields may not be empty")

  override def mapScript: Script = {
    val fieldPrefix = s"$PARAMS.$SOURCE."
    val fieldSuffix = ".hashCode()"
    val fieldsFormula = sourceFields
      .iterator
      .map { fieldPrefix + _ + fieldSuffix }
      .mkString(" + ")
    new Script(s"$PARAMS.$AGG.$HASHES.add( $fieldsFormula )")
  }

}
