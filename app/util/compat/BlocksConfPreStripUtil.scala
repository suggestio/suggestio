package util.compat

import io.suggest.util.JMXBase
import io.suggest.ym.model.MAd
import io.suggest.ym.model.common.BlockMeta
import util.PlayMacroLogsImpl
import util.blocks.BlocksConf
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.10.14 9:37
 * Description: Перед срезанием лишних блоков надо обновить все MAd.blockMeta: выставить width и
 * потом выставить block_id = 20, чтобы не было warn'ов в логах.
 * 2014.oct.23: Потом можно будет "забыть" поле blockId, но пока этого делать нельзя.
 */
object BlocksConfPreStripUtil extends PlayMacroLogsImpl {

  import LOGGER._


  /** Обновить blockMeta у всех рекламных карточек с помощью функции. */
  private def updateAllBlockMeta(f: BlockMeta => BlockMeta): Future[Int] = {
    MAd.updateAll() { mad0 =>
      val mad1 = mad0.copy(
        blockMeta = f(mad0.blockMeta)
      )
      Future successful mad1
    }
  }

  /**
   * Выставить всем карточкам указанный blockMeta.blockId.
   * @param newBlockId Новое значение blockId.
   * @return Фьючерс с кол-вом обновлённых карточек.
   */
  def resetAllMadBmBlockId(newBlockId: Int): Future[Int] = {
    warn("resetAllMadBmBlockId(): Starting...")
    updateAllBlockMeta { bm0 =>
      bm0.copy(
        blockId = newBlockId
      )
    }
  }

}


/** MBean-интерфейс для JMX-реализации. */
trait BlocksConfPreStripUtilJmxMBean {
  def resetAllMadBmBlockId(newBlockId: Int): String
  def resetAllMadBmBlockIdToFirst(): String
}

/** JMX MBean реализация. */
class BlocksConfPreStripUtilJmx extends JMXBase with BlocksConfPreStripUtilJmxMBean {

  override def jmxName = "io.suggest.model:type=compat,name=" + getClass.getSimpleName.replace("Jmx", "")

  override def resetAllMadBmBlockId(newBlockId: Int): String = {
    if (BlocksConf.values.find(_.id == newBlockId).nonEmpty) {
      val resFut = BlocksConfPreStripUtil.resetAllMadBmBlockId(newBlockId)
        .map { count => "Total updated: " + count }
      awaitString(resFut)
    } else {
      "Block id not found in BlocksConf: " + newBlockId
    }
  }

  override def resetAllMadBmBlockIdToFirst(): String = {
    val newBlockId = BlocksConf.values.head.id
    resetAllMadBmBlockId(newBlockId)
  }

}
