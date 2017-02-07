package controllers

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import util.acl.IsAdnNodeAdmin

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.02.17 16:06
  * Description: Контроллер для системы управления деревом узлов.
  * Появился в контексте необходимости системы управления собственными маячками.
  * Маячки -- очень частный случай под-узла, поэтому тут скорее управление ресиверами.
  *
  * Ещё есть необходимость размещаться в маячках. Форма может работать как в контексте карточки,
  * так и в контексте узла.
  *
  * Контроллер также должен препятствовать нежелательной деятельности пользователя:
  * - массового создания маячков с целью занять чужие id'шники.
  */
class LkNodes @Inject() (
                          isAdnNodeAdmin            : IsAdnNodeAdmin,
                          override val mCommonDi    : ICommonDi
                        )
  extends SioControllerImpl
  with MacroLogsImpl
{

  /**
    * Рендер страницы с формой управления узлами.
    * Сама форма реализована через react, тут у нас лишь страничка-обёртка.
    *
    * @param nodeId id узла, с которым идёт взаимодействие.
    * @return 200 + HTML, если у юзера достаточно прав для управления узлом.
    */
  def onNode(nodeId: String) = isAdnNodeAdmin.Get(nodeId).async { implicit request =>
    ???
  }

}
