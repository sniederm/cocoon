/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
/*
 * Created on 08-oct-2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package javax.jdo;

abstract public interface Transaction {
	abstract public void begin();
	abstract public void commit();
	abstract public boolean getNonTransactionlaRead();
	abstract public boolean getNonTransactionlaWrite();
	abstract public boolean getOptimistic();
	abstract public PersistenceManager getPersistenceManager();
	abstract public boolean getRestoreValues();
	abstract public boolean getRetainValues();
//	abstract public Synchronization getSynchronization();
	abstract public boolean isActive();
	abstract public void rollback();
	abstract public void setNontransactionalRead(boolean b);
	abstract public void setNontransactionalWrite(boolean b);
	abstract public void setOptimistic(boolean b);
	abstract public void setRestoreValues(boolean b);
	abstract public void setRetainValues(boolean b);
//	abstract public void setSynchronization(Synchronization s);
}

