package controllers

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 17.05.13
 * Time: 17:03
 * To change this template use File | Settings | File Templates.
 */


import models.MBlog
import play.api._
import play.api.mvc._
import util.{AclT, ContextT}
import views.html.blog._




object Blog extends Controller with ContextT with AclT {

  /**
   * Функция запроса всех записей блога
   * @return
   */

  def recordList = maybeAuthenticated { implicit pw_opt => implicit request =>
    val blogs = MBlog.getAll
    Ok(listTpl(blogs))
  }

  /**
   *Функция запроса запис в блоге по id.
   * @param blog_id
   * @return
   */

  def readOne(blog_id: String) = maybeAuthenticated { implicit pw_opt => implicit request =>
    val recordOpt = MBlog.getById(blog_id)
    recordOpt match {
    case Some(record) =>
    Ok(_blogRecordTpl(record))
    case None =>
    NotFound
    }
  }

  /**
   * Функция уудаление записи в блоге.
   * @param rec_id
   * @return
   */

  def deleteRecAdmin (rec_id: String) =  isAdminAction { implicit pw_opt => implicit request =>
    val deleteRec = MBlog.delete(rec_id)
    Ok(deleteRec.toString)
  }

}