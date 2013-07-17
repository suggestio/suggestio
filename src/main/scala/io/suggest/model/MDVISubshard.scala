package io.suggest.model

import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.{SioEsIndexUtil, Logs}
import org.elasticsearch.client.Client
import io.suggest.model.MVirtualIndex
import io.suggest.index_info.MDVIUnit

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 17:08
 * Description: Виртуальная подшарда для индекса. Используется для группировки индексированных страниц во времени.
 * Управление номерами шард сделано для возможных случаев, когда необходимо или же наоборот нежелательно
 * партициирование того или иного типа. Например, в целях экономии RAM при индексировании данных, для которых
 * нежелательно партициирование данных из-за неустранимого long-tail в текстовом индексе (куча редких слов).
 * @param dvin Родительский экземпляр MDVIActive.
 * @param lowerDateDays Нижняя дата этой подшарды в днях.
 * @param shards Номера задействованных шард в elasticsearch. Если Nil, то значит нужно опрашивать
 *               всю родительскую виртуальную шарду.
 */
case class MDVISubshard(
  dvin:          MDVIUnit,
  lowerDateDays: Int,
  shards:        List[Int] = Nil
) extends Logs {

  /**
   * Получить список названий шард на основе их id'шников.
   */
  lazy val getShards: Seq[String] = {
    if (shards.isEmpty) {
      dvin.getShards
    } else {
      val vin = dvin.vin
      shards.map(MVirtualIndex.vinAndCounter2indexName(vin, _))
    }
  }


  /**
   * Вернуть тип, который будет адресоваться в рамках ES.
   * @return Строка типа индекса. Например "suggest.io-123123".
   */
  def typename: String = dvin.dkey + "-" + lowerDateDays


  /**
   * Выставить маппинг в es для всех индексов.
   * @param indices Список индексов. По дефолту запрашивается у виртуального индекса.
   * @param failOnError Сдыхать при ошибке. По дефолту true.
   * @return фьючерс с isAcknowledged.
   */
  def setMappings(indices:Seq[String] = getShards, failOnError:Boolean = true)(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    SioEsIndexUtil.setMappingsFor(indices, typename, failOnError)
  }

  /**
   * Удалить маппинги (вероятно, содержащие данные), созданные из setMappings.
   * @param indices индексы. По дефолту взять из файла данных.
   * @return Выполненный фьючерс, если всё нормально.
   */
  def deleteMappaings(indices:Seq[String] = getShards)(implicit client:Client, executor:ExecutionContext): Future[Unit] = {
    SioEsIndexUtil.deleteMappingsSeqFrom(indices, Seq(typename))
  }

}

