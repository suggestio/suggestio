package models.mtag

import io.suggest.model.n2.node.MNodeSearchDfltImpl
import io.suggest.model.n2.tag.MNodeTagInfo
import io.suggest.model.n2.tag.edge.ITags
import io.suggest.model.n2.tag.vertex.{MTagFace, MTagVertex}
import models._
import util.PlayMacroLogsImpl
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 16:53
 * Description: Модель для некоторых высокоуровневых над содержимым MNode.tag.
 */
object MTagUtil extends PlayMacroLogsImpl {

  import LOGGER._

  /**
   * На каком-то узле, использующем указатели на теги, прозошло обновление с одних тегов на другие,
   * и нужно это отразить в модели узлов-тегов.
   * @param oldTags Старый список тегов, если они есть.
   * @param newTags Новый набор тегов.
   * @return Фьючерс.
   */
  def handleNewTagsL(newTags: Iterable[MTagEdge], oldTags: Iterable[MTagEdge] = Nil)
                    (implicit ec: ExecutionContext): Future[_] = {
    val resFut = Future.traverse( newTags ) { ntag =>
      val nodeSearch = new MNodeSearchDfltImpl {
        override def tagVxFace = Some(ntag.face)
        override def limit = 1
      }
      MNode.dynCount(nodeSearch)
        .filter { _ > 0 }
        .map { _ => Option.empty[(String, String)] }
        .recoverWith {
          case nsee: NoSuchElementException =>
            // Запрашиваемый тег ещё не существует в графе N2.
            val mnode = MNode(
              common = MNodeCommon(
                ntype         = MNodeTypes.Tag,
                isDependent   = true
              ),
              tag = MNodeTagInfo(
                vertex = Some(MTagVertex(
                  faces = MTagFace.faces2map( Seq(MTagFace(ntag.face)) )
                ))
              )
            )
            mnode.save
              .map { mnodeId => Some(ntag.face -> mnodeId) }
        }
    }
    val idsFut = resFut.map { _.flatten }

    // Логгируем результаты.
    lazy val logPrefix = s"handleNewTags(${newTags.size}): "
    idsFut onComplete {
      case Success(ids) =>
        debug(logPrefix + "Created " + ids.size + " new N2 nodes: " + ids)
      case Failure(ex) =>
        error(logPrefix + "Unable to save new nodes for tags: " + newTags, ex)
    }

    idsFut
  }

  def handleNewTagsM(newTags: TagsMap_t, oldTags: TagsMap_t = Map.empty)
                   (implicit ec: ExecutionContext): Future[_] = {
    handleNewTagsL(
      newTags = MTagEdge.map2tags( newTags ),
      oldTags = MTagEdge.map2tags( oldTags )
    )
  }

  def handleNewTagsI(newTags: ITags, oldTags: ITags)(implicit ec: ExecutionContext): Future[_] = {
    handleNewTagsM(newTags.tags,  oldTags = oldTags.tags)
  }


}
