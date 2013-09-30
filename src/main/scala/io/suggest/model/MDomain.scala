package io.suggest.model

import scala.concurrent.{ExecutionContext, Future, future}
import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor}
import org.apache.hadoop.hbase.client.{Get, Put}
import HTapConversionsBasic._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.13 11:52
 * Description: hbase-модель для domain-данных. Таблица включает в себя инфу по доменам, выраженную props CF и в др.моделях.
 */

// В трейт вынесен динамический функционал модели, чтобы легко использовать его в над-проектах.
trait MDomainT {
  def dkey: String

  def getDVIByVin(vin: String)(implicit ec: ExecutionContext) = MDVIActive.getForDkeyVin(dkey, vin)
  def getAllDVI(implicit ec: ExecutionContext) = MDVIActive.getAllForDkey(dkey)
  def getSearchPtr(idOpt:Option[String] = None)(implicit ec: ExecutionContext) = MDVISearchPtr.getForDkeyId(dkey)
  def getProp(key: String)                                = MObject.getProp(dkey, key)
  def setProp(key: String, value: Array[Byte])            = MObject.setProp(dkey, key, value)
  def getDomainSettings(implicit ec: ExecutionContext)    = DomainSettings.getForDkey(dkey)
  def getAnyDomainSettings(implicit ec:ExecutionContext)  = DomainSettings.getAnyForDkey(dkey)
}

// ActiveRecord для использования в рамках sio.util.
case class MDomain(dkey: String) extends MDomainT

