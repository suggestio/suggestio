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

  def recordList = maybeAuthenticated { implicit pw_opt => implicit request =>
    val blogs = MBlog.getAll
    Ok(listTpl(blogs))
  }

}