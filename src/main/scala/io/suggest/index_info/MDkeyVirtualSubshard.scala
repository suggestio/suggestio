package io.suggest.index_info

import scala.concurrent.Future
import io.suggest.util.{SioEsIndexUtil, Logs}
import org.elasticsearch.client.Client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 17:08
 * Description: Виртуальная подшарда для индекса. Используется для группировки индексированных страниц во времени.
 * Управление номерами шард сделано для возможных случаев, когда необходимо или же наоборот нежелательно
 * партициирование того или иного типа. Например, в целях экономии RAM при индексировании данных, для которых
 * нежелательно партициирование данных из-за неустранимого long-tail в текстовом индексе (куча редких слов).
 * @param typename Название типа в ElasticSearch.
 * @param lowerDateDays Нижняя дата этой подшарды в днях.
 * @param shards Номера задействованных шард в elasticsearch. Если Nil, то значит нужно опрашивать
 *               всю родительскую виртуальную шарду.
 */
case class MDkeyVirtualSubshard(
  typename:      String,
  lowerDateDays: Int,
  dvin:          MDVIActive,
  shards:        List[Int] = Nil
) extends Logs {

  lazy val getShards: Seq[String] = {
    shards match {
      case Nil => dvin.getShards
      case _   => shards.map()
    }
  }

  /**
   * Выставить маппинг в es для всех индексов.
   * @param indices Список индексов. По дефолту запрашивается у виртуального индекса.
   * @param failOnError Сдыхать при ошибке. По дефолту true.
   * @return фьючерс с isAcknowledged.
   */
  def setMappings(indices:Seq[String] = getShards, failOnError:Boolean = true)(implicit client:Client): Future[Boolean] = {
    SioEsIndexUtil.setMappingsFor(indices, typename, failOnError)
  }

  /**
   * Удалить маппинги (вероятно, содержащие данные), созданные из setMappings.
   * @param indices индексы. По дефолту взять из файла данных.
   * @param failOnError Останавливаться при ошибке. По дефолту - false.
   * @return true, если всё нормально.
   */
  def deleteMappaings(indices:Set[String] = getShards, failOnError:Boolean = false)(implicit client:Client): Future[Boolean] = {

  }

}

