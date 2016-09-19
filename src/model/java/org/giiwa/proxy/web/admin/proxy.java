/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.proxy.web.admin;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

public class proxy extends Model {

  @Path(login = true, access = "access.config.admin")
  public void onGet() {
    int s = this.getInt("s");
    int n = this.getInt("n", 20, "number.per.page");

    W q = W.create("model", "admin.proxy").sort("created", -1);

    Beans<OpLog> bs = OpLog.load(q, s, n);
    this.set(bs, s, n);
    this.query.path("/admin/proxy");

    this.show("/admin/proxy.log.html");
  }

  @Path(path = "setting", login = true, access = "access.config.admin")
  public void setting() {
    if (method.isPost()) {
      Global.setConfig("proxy.http.port", this.getInt("http_port"));
      Global.setConfig("proxy.socks.port", this.getInt("socks_port"));
    }
    this.show("/admin/proxy.setting.html");
  }

  @Path(path = "deleteall", login = true, access = "access.config.admin")
  public void deleteall() {
    W q = W.create("model", "admin.proxy");
    Helper.delete(q, OpLog.class);
  }

}
