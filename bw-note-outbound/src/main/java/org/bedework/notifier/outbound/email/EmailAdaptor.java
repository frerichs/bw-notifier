/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.bedework.notifier.outbound.email;

import org.bedework.caldav.util.notifications.NotificationType;
import org.bedework.caldav.util.notifications.ProcessorType;
import org.bedework.notifier.Action;
import org.bedework.notifier.exception.NoteException;
import org.bedework.notifier.notifications.Note;
import org.bedework.notifier.outbound.common.AbstractAdaptor;
import org.bedework.util.http.HttpUtil;

import javax.xml.namespace.QName;

/** The interface implemented by destination adaptors. A destination
 * may be an email address or sms.
 * <?xml version="1.0" encoding="UTF-8" ?>
 <CSS:notification xmlns:C="urn:ietf:params:xml:ns:caldav"
                   xmlns:BSS="http://bedework.org/ns/"
                   xmlns:BW="http://bedeworkcalserver.org/ns/"
                   xmlns:CSS="http://calendarserver.org/ns/"
                   xmlns:DAV="DAV:">
   <BSS:processors>
     <BSS:processor>
       <BSS:type>email</BSS:type>
     </BSS:processor>
   </BSS:processors>
   <CSS:dtstamp>20150819T173132Z</CSS:dtstamp>
   <CSS:invite-notification shared-type="calendar">
     <BW:name>402881f4-4f470345-014f-47040de1-00000004</BW:name>
     <CSS:uid>402881f4-4f470345-014f-47040de1-00000004</CSS:uid>
     <DAV:href>mailto:douglm@mysite.edu</DAV:href>
     <CSS:invite-noresponse/>
     <CSS:access>
       <CSS:read-write/>
     </CSS:access>
     <CSS:hosturl>
      <DAV:href>/notifyws/user/mtwain/share</DAV:href>
     </CSS:hosturl>
     <CSS:organizer>
       <DAV:href>mailto:mtwain@mysite.edu</DAV:href>
       <CSS:common-name></CSS:common-name>
     </CSS:organizer>
     <CSS:summary>share</CSS:summary>
     <C:supported-calendar-component-set>
       <C:comp name="VEVENT"/>
       <C:comp name="VTODO"/>
       <C:comp name="VAVAILABILITY"/>
     </C:supported-calendar-component-set>
   </CSS:invite-notification>
 </CSS:notification>

 *
 * @author Greg Allen
 * @author Mike Douglass
 *
 */
public class EmailAdaptor extends AbstractAdaptor<EmailConf> {
  final static String processorType = "email";

	private Mailer mailer;

	@Override
	public boolean process(final Action action) throws NoteException {
    final Note note = action.getNote();
    final NotificationType nt = note.getNotification();
    final EmailSubscription sub = EmailSubscription.rewrap(action.getSub());
    final ProcessorType pt = getProcessorStatus(note, processorType);

    if (processed(pt)) {
      return true;
    }

    final EmailMessage email =
            new EmailMessage(conf.getFrom(), null);

    /* The subscription will define one or more recipients */
    for (final String emailAddress: sub.getEmails()) {
      email.addTo(stripMailTo(emailAddress));
    }

		// if (note.isRegisteredRecipient()) {
    //   do one thing
    // ? else { ... }

    final QName elementName =  nt.getNotification().getElementName();
    String prefix = nsContext.getPrefix(elementName.getNamespaceURI());

    if (prefix == null) {
      prefix = "default";
    }

    String subject = getConfig().getSubject(prefix + "-" +
                                                    elementName.getLocalPart());
    if (subject == null) {
      subject = getConfig().getDefaultSubject();
    }
    email.setSubject(subject);

    email.addBody(EmailMessage.CONTENT_TYPE_PLAIN,
                  applyTemplate(elementName,
                                Note.DeliveryMethod.email,
                                nt,
                                note.getExtraValues()));

    try {
      getMailer().send(email);

      pt.setDtstamp(getDtstamp());
      pt.setStatus(HttpUtil.makeOKHttpStatus());

      return true;
    } catch (final NoteException ne) {
      if (debug) {
        error(ne);
      }

      return false;
    }
	}

  private Mailer getMailer() throws NoteException {
		if (mailer == null) {
			mailer = new Mailer(getConfig());
		}
		return mailer;
	}
}
