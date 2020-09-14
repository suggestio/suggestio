package io.suggest.lk.nodes.form.m

import io.suggest.scalaz.NodePath_t
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.08.2020 9:28
  * Description: Модель пропертисов react-компонентов вокруг MNodeState при рендере в дереве.
  * Используется несколькими компонентами, поэтому вынесена за пределы компонентов.
  */
object MNodeStateRender {

  implicit def NodeStateRenderFeq = FastEqUtil[MNodeStateRender] { (a, b) =>
    // node state хранится в состоянии и редко изменяется.
    (a.state ===* b.state) &&
    // Путь пере-собирается при каждом render(), поэтому требуется полное сравнивание.
    // Сверка пути нужна только для генерации tree nodeId внутри NodeR().render().TreeItem(props.nodeId)
    (a.rawNodePathRev ==* b.rawNodePathRev)
  }

  @inline implicit def univEq: UnivEq[MNodeStateRender] = UnivEq.derive

  /** Собрать путь до узла в дереве в правильном порядке.
    * Используем def для экономии ресурсов: nodePathRev шарит свои хвосты между с другими элементами,
    * а прямой nodePath нужен только на момент срабатывания какого-то экшена.
    */
  def unRawNodePathRev( rawNodePathRev: NodePath_t ): NodePath_t =
    rawNodePathRev.reverse.tail


  implicit class MnsrOpsExt( private val mnsr: MNodeStateRender ) extends AnyVal {

    def nodePath: NodePath_t =
      unRawNodePathRev( mnsr.rawNodePathRev )

  }

}


case class MNodeStateRender(
                             state                : MNodeState,
                             rawNodePathRev       : NodePath_t,
                           )

