package util.up

import java.io.{File, FileInputStream}

import javax.inject.Inject
import io.suggest.crypto.hash.{MHash, MHashes}
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.n2.edge.MEdge
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.media.storage.IMediaStorages
import io.suggest.n2.media.{MEdgeMedia, MFileMetaHash, MFileMetaHashFlag}
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.util.logs.MacroLogsImpl
import org.apache.commons.codec.digest.DigestUtils
import play.api.inject.Injector
import io.suggest.ueq.UnivEqUtil._
import io.suggest.common.empty.OptionUtil.BoolOptOps

import scala.concurrent.blocking
import scala.concurrent.{ExecutionContext, Future}

// TODO Унести в [util] или ещё куда-нибудь. Нет необходимости держать это в [www].

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 11:49
 * Description: Утиль для работы с файлами в файловой системе.
 */
class FileUtil @Inject()(
                          injector: Injector,
                        )
  extends MacroLogsImpl
{

  // Ленивое DI для некоторых методов.
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]
  private lazy val iMediaStorages = injector.instanceOf[IMediaStorages]


  /**
    * Рассчитать SHA-1 для файла.
    *
    * @param file Исходный файл.
    * @return Строка чексуммы вида "bf35fa420d3e0f669e27b337062bf19f510480d4".
    * @see Написано по мотивам [[http://stackoverflow.com/a/2932513]].
    */
  def sha1(file: File): String = {
    _hashStream( file )( DigestUtils.sha1Hex )
  }

  def sha256(file: File): String = {
    _hashStream( file )( DigestUtils.sha256Hex )
  }

  def mkFileHash(mhash: MHash, file: File): String = {
    mhash match {
      case MHashes.Sha1   => sha1(file)
      case MHashes.Sha256 => sha256(file)
    }
  }

  def mkHashesHexAsync(file: File, hashes: Iterable[MHash], flags: Set[MFileMetaHashFlag]): Future[Seq[MFileMetaHash]] = {
    for {
      kvs <- Future.traverse(hashes) { mhash =>
        Future {
          MFileMetaHash(
            hType     = mhash,
            hexValue  = mkFileHash(mhash, file),
            flags     = flags
          )
        }
      }
    } yield {
      kvs.toSeq
    }
  }

  private def _hashStream(file: File)(f: FileInputStream => String): String = {
    blocking {
      val is = new FileInputStream(file)
      try {
        f(is)
      } finally {
        is.close()
      }
    }
  }



  /** Надо ли стирать файл из хранилища?
    * Надо, если этот файл точно больше нигде не используется.
    *
    * @param edge4delete
    * @param mnode Узел. Если удаление эджа, то исходный узел.
    *              Если редактирование эджа, то УЖЕ ОТРЕДАКТИРОВАННЫЙ узел.
    * @param reportDupEdge Надо ли ругаться в логи, если файл всё ещё существует в узле?
    * @return None - удалять ничего не надо.
    *         Some() - надо удалить указанный файл.
    */
  def isNeedDeleteFile(edge4delete: MEdge,
                       mnode: MNode,
                       reportDupEdge: Boolean): Future[Option[MEdgeMedia]] = {
    lazy val logPrefix = s"isNeedDeleteFile(${edge4delete.predicate} ${edge4delete.media.flatMap(_.storage).fold("")(_.data.meta)}, node#${mnode.idOrNull}, dup?$reportDupEdge):"

    (for {
      edgeMediaDelete <- edge4delete.media
      storageDelete <- edgeMediaDelete.storage
      // Если нельзя это удалять физически, то и шерстить смысла нет.
      if {
        val r = storageDelete.storage.canDelete
        if (!r)
          LOGGER.debug(s"$logPrefix Edge have file ${storageDelete.data.meta}, but delete operation is not supported.")
        r
      }
    } yield {
      // Если есть ещё file-эджи внутри текущего узла, то поискать.
      // Вообще, других эджей быть не должно, но всё-равно страхуемся от ошибочных дубликатов эджа...
      val nodeHasSameFileEdge = (for {
        nodeEdge          <- mnode.edges.out.iterator
        if nodeEdge !===* edge4delete
        nodeEdgeMedia     <- nodeEdge.media.iterator
        nodeEdgeMediaStorage <- nodeEdgeMedia.storage.iterator
        if nodeEdgeMediaStorage isSameFile storageDelete
      } yield {
        def msg = s"$logPrefix node#${mnode.idOrNull} contains file-node. Will NOT delete file."

        if (reportDupEdge)
          LOGGER.warn(s"$msg\n Duplicated edge:\n  $edge4delete\n duplicate edges:\n  $nodeEdgeMedia")
        else
          LOGGER.debug(msg)

        true
      })
        .nextOption()
        .getOrElseFalse

      if (nodeHasSameFileEdge) {
        Future.successful( None )
      } else {
        import esModel.api._

        // Как и ожидалось, эджа с файлом внутри текущего узла нет. Поискать среди других узлов.
        LOGGER.trace(s"$logPrefix Search for ${edgeMediaDelete.storage} in other nodes...")
        for {
          isFileExistElsewhereIds <- mNodes.dynSearchIds(
            new MNodeSearch {
              override val outEdges: MEsNestedSearch[Criteria] = {
                val cr = Criteria(
                  fileStorType      = Set.empty + storageDelete.storage,
                  fileStorMetaData  = Set.empty + storageDelete.data.meta,
                )
                MEsNestedSearch(
                  clauses = cr :: Nil,
                )
              }
              override val withoutIds = mnode.id.toList
              override def limit = 1
            }
          )
        } yield {
          val r = isFileExistElsewhereIds.isEmpty

          if (r) LOGGER.trace(s"$logPrefix Stored file ${edgeMediaDelete.storage} is not used anymore.")
          else LOGGER.warn(s"$logPrefix File ${edgeMediaDelete.storage} will not erased from storage: it is used on node ${isFileExistElsewhereIds.mkString(" ")}")

          Option.when( r )( edgeMediaDelete )
        }
      }
    })
      .getOrElse( Future.successful(None) )
  }


  /** Произвести удаления файла.
    *
    * @param media4deleteOpt Выхлоп _isNeedDeleteFile()
    * @return Фьючерс с каким-то неопределённым результатом удаления.
    */
  def deleteFileMaybe(media4deleteOpt: Option[MEdgeMedia]): Future[_] = {
    media4deleteOpt.fold [Future[_]] {
      Future.successful(None)
    }( deleteFile )
  }

  /** Произвести удаления файла.
    *
    * @param media4delete MEdgeMedia.
    * @return Фьючерс с каким-то неопределённым результатом удаления.
    */
  def deleteFile(media4delete: MEdgeMedia): Future[_] = {
    media4delete.storage.fold [Future[_]] ( Future.successful(()) ) { storage =>
      LOGGER.info(s"_deleteFile(${storage.data.meta}) Erasing file $media4delete from storage")
      iMediaStorages
        .client( storage.storage )
        .delete( storage.data )
    }
  }

}
