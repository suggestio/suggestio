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

  implicit lazy val NodeStateRenderFeq = FastEqUtil[MNodeStateRender] { (a, b) =>
    (a.state ===* b.state) &&
    // Остальные куски пересобираются на каждый чих:
    (a.rawNodePathRev ==* b.rawNodePathRev) &&
    (a.chCount ==* b.chCount) &&
    (a.chCountEnabled ==* b.chCount)
  }

  @inline implicit def univEq: UnivEq[MNodeStateRender] = UnivEq.derive

}


case class MNodeStateRender(
                             state                : MNodeState,
                             rawNodePathRev       : NodePath_t,
                             chCount              : Int,
                             chCountEnabled       : Int,
                           ) {

  /** Путь до узла в правильном порядке.
    * Используем def для экономии ресурсов: nodePathRev шарит свои хвосты между с другими элементами,
    * а прямой nodePath нужен только на момент срабатывания какого-то экшена. */
  def nodePath = rawNodePathRev.reverse.tail

}

