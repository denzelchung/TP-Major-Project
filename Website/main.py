#!/usr/bin/env python
#
# Copyright 2007 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
from database import *

def make_secure_val(val):
    return '%s|%s' % (val, hmac.new(secret, val).hexdigest())

def check_secure_val(secure_val):
    val = secure_val.split('|')[0]
    if secure_val == make_secure_val(val):
        return val

class Handler(webapp2.RequestHandler):
    def write(self, *a, **kw):
        self.response.out.write(*a, **kw)

    def render_str(self, template, **params):
        params['user'] = self.user
        t = jinja_env.get_template(template)
        return t.render(params)

    def render(self, template, **kw):
        kw['psi_check'] = self.twenty_four_hour
        kw['holidays'] = self.holidays
        kw['datetime_now'] = self.datetime_now
        kw['holiday_limit'] = datetime.timedelta(days=10)
        nonce = id_generator()
        #if template == "rain-map.html":
        #    self.response.headers.add("Content-Security-Policy", "default-src 'self' ajax.googleapis.com; script-src 'unsafe-eval' 'unsafe-inline' 'self' www.google-analytics.com ajax.googleapis.com *.addthis.com maps.googleapis.com maps.gstatic.com *.googleapis.com 'nonce-%s'; connect-src 'self'; img-src 'self' s3-ap-southeast-1.amazonaws.com/cenplusplus/ www.google-analytics.com stats.g.doubleclick.net maps.gstatic.com weather.nea.gov.sg *.gstatic.com *.googleapis.com; style-src 'self' *.addthis.com fonts.googleapis.com; font-src 'self' fonts.gstatic.com; frame-src 'self' *.addthis.com" % nonce)
        self.response.headers.add("Content-Security-Policy",
            "default-src 'self' ajax.googleapis.com; \
            script-src 'unsafe-eval' 'self' www.google-analytics.com \
                ajax.googleapis.com *.addthis.com https://maps.googleapis.com \
                https://maps.gstatic.com *.googleapis.com \
                www.google.com *.twitter.com \
                cdn.syndication.twimg.com 'nonce-%s'; \
            connect-src 'self'; \
            img-src *; \
            style-src 'self' platform.twitter.com www.google.com \
                *.addthis.com fonts.googleapis.com 'nonce-%s'; \
            font-src 'self' fonts.gstatic.com; \
            frame-src 'self' *.twitter.com *.addthis.com" % (nonce, nonce))

##        img-src 'self' http://d1edc2swgisknk.cloudfront.net/ \
##                www.google-analytics.com *.addthis.com \
##                stats.g.doubleclick.net maps.gstatic.com \
##                weather.nea.gov.sg *.gstatic.com *.googleapis.com; \
        kw['nonce'] = nonce
        self.write(self.render_str(template, **kw))

    def render_json(self, d):
        json_txt = json.dumps(d)
        self.response.headers['Content-Type'] = 'application/json; charset=UTF-8'
        self.write(json_txt)

    def set_secure_cookie(self, name, val):
        cookie_val = make_secure_val(val)
        self.response.headers.add_header(
            'Set-Cookie',
            '%s=%s; Path=/' % (name, cookie_val))

    def read_secure_cookie(self, name):
        cookie_val = self.request.cookies.get(name)
        return cookie_val and check_secure_val(cookie_val)

    def login(self, user):
        self.set_secure_cookie('user_id', str(user.key().id()))

    def logout(self):
        self.response.headers.add_header('Set-Cookie', 'user_id=; Path=/')

    def initialize(self, *a, **kw):
        webapp2.RequestHandler.initialize(self, *a, **kw)
        uid = self.read_secure_cookie('user_id')
        self.user = uid and User.by_id(int(uid))
        self.user_type = uid and User.get_user_type(int(uid))
        if self.request.path.endswith('.json'):
            self.format = 'json'
        else:
            self.format = 'html'
        try:
            self.three_hour, self.twenty_four_hour = get_haze_status()
        except:
            logging.error("Failed to initialize PSI levels")
        self.holidays = get_holidays()
        self.datetime_now = datetime.datetime.now()
##        Holidays(parent=holiday_key,
##                 name="Hari Raya Puasa",
##                 day="Monday",
##                 date=datetime.datetime(2014, 7, 28, 0, 0, 0)).put()
##        get_holidays(True)

    def error_404(self):
        ## TODO
        self.write("404 ERROR")

    def error_invalid_user(self):
        ## TODO
        self.write("Invalid User")

    def error_invalid_report(self):
        ## TODO
        self.write("Invalid Report")

    def error_invalid_garden(self):
        ## TODO
        self.write("Invalid Area")


class RainMap(Handler):
    def get(self):
        self.render('rain-map.html')

def get_rain_areas(update = False):
    key = 'rain_areas'
    rain_areas = memcache.get(key)
    if rain_areas is None or update:
        logging.error("DB QUERY")
        rain_areas = db.GqlQuery("SELECT * "
                                 "FROM RainArea "
                                 "WHERE ANCESTOR IS :1 "
                                 "ORDER BY area ASC",
                                 rain_area_key)
        rain_areas = list(rain_areas)
        memcache.set(key, rain_areas)
    return rain_areas

def get_rain_maintenance(maintenance = False):
    key = 'rain_maintenance'
    rain_maintenance = memcache.get(key)
    if rain_maintenance is None or maintenance:
        memcache.set(key, maintenance)
    return maintenance

class RainLocation(Handler):
    def get(self):
        rain_picture = db.Blob(urlfetch.Fetch(rain_page).content)
        if rain_picture:
            #areas = RainArea.all().order("area")
            radar_maintenance = False
            #logging.info(areas.count())

            #radar_maintenance = refresh_rain_areas(areas, im)
            # refresh every 5 minute
##            if not areas.get() or \
##                (datetime.datetime.now().minute - areas[0].loaded.minute) >= 5 or \
##                (datetime.datetime.now().hour != areas[0].loaded.hour):
##                radar_maintenance = segment_rain_img(im)
            areas = get_rain_areas(True)
            radar_maintenace = get_rain_maintenance()
            # convert to Singapore (GMT+8) timezone
            last_updated = areas[0].loaded + datetime.timedelta(hours = 8)

            #areas = list(areas)
            xmlurl = "http://www.weather.gov.sg/wip/pp/rndops/web/rss/rssForecast_new.xml"
            xml = urllib.urlopen(xmlurl)
            dom = minidom.parse(xml)
            forecast_summary = dom.getElementsByTagName("summary")[0].firstChild.nodeValue
            forecast_summary = remove_html(forecast_summary).strip()

            from_to = forecast_summary[:forecast_summary.find("Updated")].strip()
            forecast = forecast_summary[forecast_summary.find("Forecast"):forecast_summary.find("Max")].strip()
            temperature = forecast_summary[forecast_summary.find("Max"):forecast_summary.find("Relative")].strip()
            humidity = forecast_summary[forecast_summary.find("Relative"):forecast_summary.find("High")].strip()

            
            self.render('rain.html', rain_img=rain_page,
                            areas=areas,
                            last_updated=last_updated,
                            radar_maintenance=radar_maintenance,
                            from_to=from_to,
                            forecast=forecast,
                            temperature=temperature,
                            humidity=humidity)
        else:
            logging.error("NEA Rain Image not available.")
        #self.render('rain.html')

class UpdateRainLocation(Handler):
    def get(self):
        img_file = cStringIO.StringIO(urllib.urlopen(rain_page).read())
        im = Image.open(img_file)
        radar_maintenance = segment_rain_img(im)
        get_rain_areas(True)
        if radar_maintenance:
            get_rain_maintenance(radar_maintenance)
        

def get_haze_status(update = False):
    key = 'haze_status'
    haze_status = memcache.get(key)
    if haze_status is None or update:
        logging.error("TWITTER API QUERY: HAZE")
        try:
            three_hour = None
            twenty_four_hour = None
            api = TwitterAPI(
                consumer_key,
                consumer_secret,
                access_token_key,
                access_token_secret,
                auth_type='oAuth2')
            ## TODO: demo
            nea_haze = api.request('statuses/user_timeline',
                                   {'screen_name': 'cenplusplus',
                                    'count': 25})
##            nea_haze = api.request('statuses/user_timeline',
##                                   {'user_id': 59072662,
##                                    'screen_name': 'NEAsg',
##                                    'count': 25})
            if nea_haze.status_code == 200:
                for tweet in nea_haze:
                    text = tweet['text']
                    psi_hr = tweet['text'].split(" ")[0]
                    if not three_hour and text:
                        if psi_hr == '3-hour':
                            time = text.split(" ")[3]
                            psi = text.split(" ")[5][:-1]
                            three_hour = (time, psi)
                    if not twenty_four_hour:
                        if psi_hr == '24-hour':
                            time = text.split(" ")[3]
                            psi = text.split(" ")[5][:-1]
                            twenty_four_hour = (time, psi)
            haze_status = (three_hour, twenty_four_hour)
            memcache.set(key, haze_status)
        except:
            logging.error("Failed to retrieve Haze Status")
    return haze_status

class Haze(Handler):
    def get(self):
        try:
            three_hour, twenty_four_hour = get_haze_status()
        except:
            logging.error("Failed to initialize PSI levels")
        self.render("haze.html", psi_three=three_hour,
                                 psi_twenty_four=twenty_four_hour)

class UpdateHaze(Handler):
    def get(self):
        get_haze_status(True)


class Signup(Handler):
    def get(self):
        # check if user is logged in
        if self.user:
            self.redirect('/')
        else:
            self.render("signup-form.html")

    def post(self):
        have_error = False
        self.username = self.request.get('username')
        self.password = self.request.get('password')
        self.verify = self.request.get('verify')
        self.email = self.request.get('email')
        self.phone = self.request.get('phone')
        self.gender = self.request.get('gender')
        self.location = self.request.get('location')
        self.photo = self.request.get('photo')
        
        params = dict(username = self.username,
                      email = self.email,
                      phone = self.phone,
                      gender = self.gender,
                      location = self.location)

        if not valid_username(self.username):
            params['error_username'] = "Invalid username"
            have_error = True

        if not valid_password(self.password):
            params['error_password'] = "Invalid password"
            have_error = True
        elif self.password != self.verify:
            params['error_verify'] = "Your passwords didn't match"
            have_error = True

        if not valid_email(self.email):
            params['error_email'] = "Invalid email"
            have_error = True

        if self.phone == "":
            params['error_phone'] = "Field cannot be empty"
            have_error = True

        if self.gender == "":
            params['error_gender'] = "Select your gender"
            have_error = True

        if have_error:
            self.render('signup-form.html', **params)
        else:
            self.done(**params)

    def done(self, *a, **kw):
        raise NotImplementedError

class Register(Signup):
    def done(self, **params):
        #make sure the user doesn't already exist
        u = User.by_name(self.username)
        if u:
            params['error_username'] = 'Username is Taken'
            self.render('signup-form.html', **params)
        else:
            u = User.by_email(self.email)
            if u:
                params['error_email'] = 'Email Address Already in Use'
                self.render('signup-form.html', **params)
            else:
                u = User.register(self.username,
                                  self.password,
                                  self.email,
                                  self.phone,
                                  self.gender,
                                  self.location,
                                  self.photo)
                u.put()

                self.login(u)
                self.redirect('/')
                # TODO: redirect to previous page

class Login(Handler):
    def get(self):
        # check if user is already logged in
        if self.user:
            self.redirect('/')
        else:
            self.render('login-form.html')

    def post(self):
        username = self.request.get('username')
        password = self.request.get('password')
        u = User.login(username, password)
        if u:
            self.login(u)
            self.redirect('/%s' % username)
        else:
            msg = 'Invalid login'
            self.render('login-form.html', error = msg)

class Logout(Handler):
    def get(self):
        self.logout()
        self.redirect('/')

class Profile(Handler):
    def get(self, username, page):
        user = User.by_name(username)
        if user:
            if page is None:
                photo = None
                if user.picture:
                    photo = "/img?img_id=%s" % user.key()
                all_reports = dict()
                
                people_reports = db.GqlQuery("SELECT * "
                                             "FROM MissingPerson "
                                             "WHERE ANCESTOR IS :1 "
                                             "AND user = :2 "
                                             "ORDER BY created DESC ",
                                             missing_people_key,
                                             user.username)
                people_reports = list(people_reports)
                for report in people_reports:
                    all_reports[report] = "People"

                pets_reports = db.GqlQuery("SELECT * "
                                           "FROM MissingPet "
                                           "WHERE ANCESTOR IS :1 "
                                           "AND user = :2 "
                                           "ORDER BY created DESC ",
                                           missing_pet_key,
                                           user.username)
                pets_reports = list(pets_reports)
                for report in pets_reports:
                    all_reports[report] = "Pets"

                belongings_reports = db.GqlQuery("SELECT * "
                                                 "FROM MissingBelonging "
                                                 "WHERE ANCESTOR IS :1 "
                                                 "AND user = :2 "
                                                 "ORDER BY created DESC ",
                                                 missing_belonging_key,
                                                 user.username)
                belongings_reports = list(belongings_reports)
                for report in belongings_reports:
                    all_reports[report] = "Belongings"
                currentUser = None
                if self.user:
                    currentUser = self.user.username
                # sort by last modified datetime descending                
                all_reports = sorted(all_reports.items(), key=getCreated, reverse=True)

                # generate QR code
                qr_contents = None
                if user.username == currentUser:
                    qr = qrcode.QRCode(
                        version=1,
                        error_correction=qrcode.constants.ERROR_CORRECT_L,
                        box_size=5,
                        border=4
                        )
                    qr.add_data(user.key().id())
                    #qr.add_data(5689792285114368)
                    qr.make(fit=True)
                    qr_img = qr.make_image()
                    output = StringIO.StringIO()
                    qr_img.save(output, "PNG")
                    qr_contents = output.getvalue().encode("base64")
                    output.close()
                
                self.render('profile.html', userPage=user,
                                            currentUser=currentUser,
                                            userType=self.user_type,
                                            photo=photo,
                                            qr=qr_contents,
                                            reports=all_reports)          
        else:
            self.error_invalid_user()

class ReportLost(Handler):
    def get(self):
        if not self.user:
            self.redirect('/signin')
        else:
            report_type = self.request.get('reportType')
            
            if not report_type:
                self.render('report-lost.html', lost_type="People")
            elif report_type in ['People', 'Pets', 'Belongings']:
                self.render('report-lost.html', lost_type=report_type)
            else:
                self.error_404()
        
    def post(self):
        if self.user:
            report_type = self.request.get('reportType')

            name = self.request.get('name')
            if report_type == "People":
                age = self.request.get('age')
            photo = self.request.get('photo')
            last_seen_at = self.request.get('lastSeenAt')
            last_seen = self.request.get('lastSeen')
            contact = self.request.get('contact')
            additional_details = self.request.get('additional')
            
            params = dict(lost_type = report_type,
                          name = name,
                          last_seen_at = last_seen_at,
                          last_seen = last_seen,
                          contact = contact,
                          additional_details = additional_details)

            if name == "":
                params['error_name'] = "Field cannot be empty"
            if report_type == "People" and age == "":
                params['error_age'] = "Field cannot be empty"
            if last_seen_at == "":
                params['error_last_seen_at'] = "Field cannot be empty"
            if last_seen == "":
                params['error_last_seen'] = "Field cannot be empty"
            if contact == "":
                params['error_contact'] = "Field cannot be empty"
            if report_type == "People":
                if not (name and age and last_seen_at and last_seen and contact):
                    params['age'] = age
                    self.render('report-lost.html', **params)
                else:
                    new_lost_report = MissingPerson(parent=missing_people_key,
                                                    name=name,
                                                    age=age,
                                                    lastSeenAt=last_seen_at,
                                                    lastSeen=last_seen,
                                                    contactDetails=contact,
                                                    additionalDetails=additional_details,
                                                    user=self.user.username)
                    if photo:
                        photo = images.resize(photo, width=250, height=300)
                        new_lost_report.picture = db.Blob(photo)
                    new_lost_report.put()
                    get_missing_people(True)
                    self.redirect('/lost/people/%s' % str(new_lost_report.key().id()))
            elif report_type == "Pets":
                if not (name and last_seen_at and last_seen and contact):
                    self.render('report-lost.html', **params)
                else:
                    new_lost_report = MissingPet(parent=missing_pet_key,
                                                 name=name,
                                                 lastSeenAt=last_seen_at,
                                                 lastSeen=last_seen,
                                                 contactDetails=contact,
                                                 additionalDetails=additional_details,
                                                 user=self.user.username)
                    if photo:
                        photo = images.resize(photo, width=250, height=300)
                        new_lost_report.picture = db.Blob(photo)
                    new_lost_report.put()
                    get_missing_pets(True)
                    self.redirect('/lost/pets/%s' % str(new_lost_report.key().id()))
            elif report_type == "Belongings":
                if not (name and last_seen_at and last_seen and contact):
                    self.render('report-lost.html', **params)
                else:
                    new_lost_report = MissingBelonging(parent=missing_belonging_key,
                                                       name=name,
                                                       lastSeenAt=last_seen_at,
                                                       lastSeen=last_seen,
                                                       contactDetails=contact,
                                                       additionalDetails=additional_details,
                                                       user=self.user.username)
                    if photo:
                        photo = images.resize(photo, width=250, height=300)
                        new_lost_report.picture = db.Blob(photo)
                    new_lost_report.put()
                    get_missing_belongings(True)
                    self.redirect('/lost/belongings/%s' % str(new_lost_report.key().id()))
        

def get_missing_people(update = False):
    key = 'missing_people'
    people_reports = memcache.get(key)
    if people_reports is None or update:
        logging.error("DB QUERY")
        #people_reports = MissingPerson.all().order('-created')
        people_reports = db.GqlQuery("SELECT * "
                                     "FROM MissingPerson "
                                     "WHERE ANCESTOR IS :1 "
                                     "ORDER BY created DESC ",
                                     missing_people_key)
        people_reports = list(people_reports)
        memcache.set(key, people_reports)
    return people_reports

def get_missing_pets(update = False):
    key = 'missing_pets'
    pets_reports = memcache.get(key)
    if pets_reports is None or update:
        logging.error("DB QUERY")
        #pets_reports = MissingPet.all().order('-created')
        pets_reports = db.GqlQuery("SELECT * "
                                   "FROM MissingPet "
                                   "WHERE ANCESTOR IS :1 "
                                   "ORDER BY created DESC ",
                                   missing_pet_key)
        pets_reports = list(pets_reports)
        memcache.set(key, pets_reports)
    return pets_reports

def get_missing_belongings(update = False):
    key = 'missing_belongings'
    belongings_reports = memcache.get(key)
    if belongings_reports is None or update:
        logging.error("DB QUERY")
        #belongings_reports = MissingBelonging.all().order('-created')
        belongings_reports = db.GqlQuery("SELECT * "
                                         "FROM MissingBelonging "
                                         "WHERE ANCESTOR IS :1 "
                                         "ORDER BY created DESC ",
                                         missing_belonging_key)
        belongings_reports = list(belongings_reports)
        memcache.set(key, belongings_reports)
    return belongings_reports

class LostNFound(Handler):
    def get(self, query):
        if query == None or not (query == '/people' or \
                             query == '/pets' or \
                             query == '/belongings'):
            self.redirect('/lost/people')
            return
            
        heading = ""
        #content = Lost.all()
        if query == '/people':
            heading = "People"
            #content = list(MissingPerson.all().order('-created'))
            content = get_missing_people()
        elif query == '/pets':
            heading = "Pets"
            #content = content.filter("lostType =", "pets").order('created')
            #content = list(MissingPet.all().order('-created'))
            content = get_missing_pets()
        elif query == '/belongings':
            heading = "Belongings"
            #content = content.filter("lostType =", "belongings").order('created')
            #content = list(MissingBelonging.all().order('-created'))
            content = get_missing_belongings()

        # get current bookmark/location
        currentPage = self.request.get('q')
        totalPages = int(math.ceil(len(content) * 1.0 / PAGE_SIZE))
        
        # get PAGE_SIZE + 1 contents
        # +1 will determine whether there is next page
        nextPage = False
        if currentPage:
            current = (int(currentPage) - 1) * PAGE_SIZE
            content = content[current:current+PAGE_SIZE+1]
            #currentPage = currentPage + PAGE_SIZE
            currentPage = int(currentPage)
        else:
            content = content[:PAGE_SIZE+1]
            currentPage = 1
        if len(content) == PAGE_SIZE + 1:
            content = content[:PAGE_SIZE]
            nextPage = True
        
        if self.format == 'html':
            currentUser = None
            if self.user:
                currentUser = self.user.username
            self.render('lost-n-found.html', nextPage=nextPage,
                                             currentPage=currentPage,
                                             numPages=totalPages,
                                             currentUser=currentUser,
                                             userType=self.user_type,
                                             heading=heading,
                                             content=content)
        elif self.format == 'json':
            return self.render_json([report.as_dict() for report in content])

class ReportPage(Handler):
    def get(self, report_type, report_id, modify_id):
        if report_type in ['people', 'pets', 'belongings']:
            report_type = report_type.capitalize()
            if report_type == 'People':
                key_path = ('MissingPerson', missing_people_key,
                            get_missing_people)
            elif report_type == "Pets":
                key_path = ('MissingPet', missing_pet_key,
                            get_missing_pets)
            elif report_type == "Belongings":
                key_path = ('MissingBelonging', missing_belonging_key,
                            get_missing_belongings)
            key = db.Key.from_path(key_path[0], int(report_id),
                                   parent=key_path[1])
            report = db.get(key)
            if not report:
                self.error_invalid_report()
                return
            if self.format == 'html':
                if self.user and \
                   (self.user.username == report.user or \
                    self.user_type == "admin"):
                    if modify_id == "/delete":
                        # delete report and update memcache
                        db.delete(report)
                        key_path[2](True)
                        self.redirect("/lost/%s" % report_type.lower())
                        return
                    elif modify_id == "/edit":
                        params = dict(name = report.name,
                                      last_seen_at = report.lastSeenAt,
                                      last_seen = report.lastSeen,
                                      contact = report.contactDetails,
                                      additional_details = report.additionalDetails)
                        if report_type == 'People':
                            params['age'] = report.age
                        self.render("report-lost-edit.html", lost_type=report_type,
                                                             report=report,
                                                             userType=self.user_type,
                                                             **params)
                        return
                    elif modify_id == "/found":
                        report.mark_as_found()
                        key_path[2](True)
                        self.redirect("/lost/%s/%s" % (report_type.lower(), report_id))
                        return
                        
                currentUser = None
                if self.user:
                    currentUser = self.user.username
                self.render("report.html", currentUser=currentUser,
                                           userType=self.user_type,
                                           heading=report_type,
                                           report=report)
            elif self.format == 'json':
                self.render_json(report.as_dict())
        else:
            self.error(404)

    def post(self, report_type, report_id, modify_id):
        if report_type in ['people', 'pets', 'belongings']:
            report_type = report_type.capitalize()
            if report_type == 'People':
                key_path = ('MissingPerson', missing_people_key,
                            get_missing_people)
            elif report_type == "Pets":
                key_path = ('MissingPet', missing_pet_key,
                            get_missing_pets)
            elif report_type == "Belongings":
                key_path = ('MissingBelonging', missing_belonging_key,
                            get_missing_belongings)
            key = db.Key.from_path(key_path[0], int(report_id),
                                   parent=key_path[1])
            report = db.get(key)
            if not report:
                self.error_invalid_report()
                return
            if self.user and \
               (self.user.username == report.user or \
                self.user_type == "admin"):
                if modify_id == "/edit":
                    new_name = None
                    if report_type == 'People':
                        new_age = None
                    new_photo = None
                    new_last_seen_at = None
                    new_last_seen = None
                    new_contact = None
                    new_additional = None

                    name = self.request.get("name")
                    if report_type == 'People':
                        age = self.request.get("age")
                    photo = self.request.get("photo")
                    last_seen_at = self.request.get("lastSeenAt")
                    last_seen = self.request.get("lastSeen")
                    contact_details = self.request.get("contact")
                    additional_details = self.request.get("additional")

                    # check if value is the same as the one in DB
                    if name != report.name:
                        new_name = name
                    if report_type == 'People' and age != report.age:
                        new_age = age
                    if photo:
                        photo = images.resize(photo, width=250, height=300)
                        new_photo = db.Blob(photo)
                        #new_photo = photo
                    if last_seen_at != report.lastSeenAt:
                        new_last_seen_at = last_seen_at
                    if last_seen:
                        new_last_seen = last_seen
                    if contact_details != report.contactDetails:
                        new_contact = contact_details
                    if additional_details != report.additionalDetails:
                        new_additional = additional_details

                    # update DB and memcache
                    if report_type == 'People':
                        report.update(new_name, new_age, new_photo,
                                      new_last_seen_at, new_last_seen,
                                      new_contact, new_additional)
                    else:
                        report.update(new_name, new_photo,
                                      new_last_seen_at, new_last_seen,
                                      new_contact, new_additional)
                    key_path[2](True)
                    self.redirect("/lost/%s/%s" % (report_type.lower(), key.id()))
                
            else:
                if modify_id == "/flag":
                    report_reason = self.request.get('flagReport')
                    other_reason = None
                    if report_reason in ['Fake Report', 'Spam', 'Wrong Category', 'Others']:
                        if report_reason == "Others":
                            other_reason = self.request.get('otherReportField')
                        if not report.flagged:
                            report.mark_flagged()
                        FlagReport(parent=flag_report_key,
                                   reportId=key.id(),
                                   report=report,
                                   reportReason=report_reason,
                                   reasonOther=other_reason).put()
                        get_flagged_reports(True)
                        self.redirect("/lost/%s/%s" % (report_type.lower(), key.id()))
                    return
                self.redirect("/signin")

class ReportViewer(Handler):
    def get(self):
        if self.user_type != "admin":
            self.error_404()
            return

        reports = get_flagged_reports()
        REPORT_LIST = list()
        REPORT_ID_LIST = list()
        for report in reports:
            reportId = report.reportId
            if reportId not in REPORT_ID_LIST:
                # search for the report in People
                for _path, _key in [('MissingPerson', missing_people_key),
                                    ('MissingPet', missing_pet_key),
                                    ('MissingBelonging', missing_belonging_key)]:
                    key = db.Key.from_path(_path, reportId,
                                           parent=_key)
                    missing_report = db.get(key)
                    if missing_report is not None:
                        REPORT_ID_LIST.append(reportId)
                        if _path == 'MissingPerson':
                            _path = 'people'
                        elif _path == 'MissingPet':
                            _path = 'pet'
                        elif _path == 'MissingBelonging':
                            _path = 'belongings'
                        REPORT_LIST.append((_path, missing_report, reportId))
                        break
                
        currentUser = None
        if self.user:
            currentUser = self.user.username
        self.render('flagged-report-view.html', reports=reports,
                                                reportList=REPORT_LIST,
                                                currentUser=currentUser,
                                                userType=self.user_type)

##                    report_reason = self.request.get('flagReport')
##                    other_reason = None
##                    if report_reason in ['Fake Report', 'Spam', 'Wrong Category', 'Others']:
##                        if report_reason == "Others":
##                            other_reason = self.request.get('otherReportField')
##                        if not report.flagged:
##                            report.mark_flagged()
##                        FlagReport(parent=flag_report_key,
##                                   reportId=key.id(),
##                                   reportReason=report_reason,
##                                   reasonOther=other_reason).put()

class ArduinoData(Handler):
    def get(self, neighbourhood, moisture, temperature, humidity, heatIndex):
        try:
            moisture = int(moisture)
            temperature = int(temperature)
            humidity = int(humidity)
            heatIndex = int(heatIndex)
            
            for obj in get_neighbourhood_list():
                if neighbourhood == obj.neighbourhood:
                    q = db.GqlQuery("SELECT * "
                                    "FROM CommunityGarden "
                                    "WHERE ANCESTOR IS :1 "
                                    "AND neighbourhood = :2",
                                    garden_key,
                                    obj)
                    garden = q.get()
                    
                    if garden:
                        if garden.neighbourhood.neighbourhood == neighbourhood:
                            points = garden.points
                            oldMoisture = garden.moistureLevel
                            oldTemperature = garden.temperature
                            oldHumidity = garden.humidity
                            oldHeatIndex = garden.heatIndex

                            updateMoisture = None
                            updateTemperature = None
                            updateHumidity = None
                            updateHeatIndex = None

                            if moisture != oldMoisture:
                                updateMoisture = moisture
                            if temperature != oldTemperature:
                                updateTemperature = temperature
                            if humidity != oldHumidity:
                                updateHumidity = humidity
                            if heatIndex != oldHeatIndex:
                                updateHeatIndex = heatIndex
                            
                            garden.updateAvg(newMoisture = moisture,
                                             newTemperature = temperature,
                                             newHumidity = humidity,
                                             newHeatIndex = heatIndex)
                            logging.info("GARDEN UPDATE")
                            garden.update(moistureLevel = moisture,
                                          temperature = temperature,
                                          humidity = humidity,
                                          heatIndex = heatIndex)
                    else:
                        # Create new record for neighbourhood
                        CommunityGarden(parent=garden_key, neighbourhood=obj,
                                        moistureLevel=moisture,
                                        temperature=temperature,
                                        humidity=humidity,
                                        heatIndex=heatIndex,
                                        avg_moisture=float(moisture),
                                        avg_temperature=float(temperature),
                                        avg_humidity=float(humidity),
                                        avg_heatIndex=float(heatIndex),
                                        avg_num=1).put()
                    get_garden_status(True)
                    return
            # Create new neighbourhood
            obj = Neighbourhoods(parent=neighbourhood_key,
                                 neighbourhood=neighbourhood)
            obj.put()
            CommunityGarden(parent=garden_key, neighbourhood=obj,
                            moistureLevel=moisture,
                            temperature=temperature,
                            humidity=humidity,
                            heatIndex=heatIndex,
                            avg_moisture=float(moisture),
                            avg_temperature=float(temperature),
                            avg_humidity=float(humidity),
                            avg_heatIndex=float(heatIndex),
                            avg_num=1).put()
            get_neighbourhood_list(True)
            get_garden_status(True)
            return
        except:
            return

class CommunityTree(Handler):
    def get(self):
        location = self.request.get('q')
        garden_status = get_garden_status()
        if location == '' and len(garden_status) > 0:
            neighbourhood = garden_status[0].neighbourhood.neighbourhood.replace(' ', '%20')
            self.redirect('/garden?q=%s' % neighbourhood)
            return
        else:
            valid = False
            for obj in get_neighbourhood_list():
                if location == obj.neighbourhood:
                    valid = True
                    break
            if not valid and location != '':
                self.error_invalid_garden()
                return
        chosenGarden = None
        for garden in garden_status:
            if garden.neighbourhood.neighbourhood == location:
                chosenGarden = garden
                break
        if chosenGarden is not None:
            self.render('community-tree.html', garden=garden_status,
                                               chosen=chosenGarden)

def get_trending(update = False):
    key = 'twitter-trending'
    trending = memcache.get(key)
    if trending is None or update:
        logging.error("TWITTER API QUERY: trending")
        api = TwitterAPI(consumer_key, consumer_secret, access_token_key, access_token_secret)
        tweets = api.request('trends/place', {'id': 23424948})
        if tweets.status_code == 200:
            trending = list()
            for tweet in tweets.get_iterator():
                trending.append((tweet['name'], tweet['url']))
            memcache.set(key, trending)

            
            return trending
    return trending


class Happenings(Handler):
    def get(self, topic):
        if topic == "news":
            self.render('happenings-news.html')
        elif topic == "twitter":
            location = ''
            if self.user:
                location = self.user.location
            trending = get_trending()
            top_tweets = list()
            #self.render('happenings-twitter.html', trending=trending)

            try:
                api = TwitterAPI(consumer_key, consumer_secret, access_token_key, access_token_secret)

                result_type = 'popular'
                q = self.request.get('q')
                if q.find('hash') == 0:
                    q = '#' + q[4:]
                elif q == 'near_me':
                    result_type = 'mixed'
                    q = location
                else:
                    inTrending = False
                    for trend in trending:
                        if trend[0] == q:
                            inTrending = True
                            break
                    if not inTrending:
                        q = 'Singapore'
                #top_trending = api.request('statuses/filter', {'track': trending[0][0]})
                top_trending = api.request('search/tweets', {'q': q,
                                                             'count': 100,
                                                             'result_type': result_type})
                if top_trending.status_code == 200:
                    for tweet in top_trending:
                        top_tweets.append(tweet)
            except:
                logging.error("No Internet Access")
            self.render('happenings-twitter.html', trending=trending,
                                                   top_trending=top_tweets,
                                                   location=location.capitalize())

class UpdateTrending(Handler):
    def get(self):
        get_trending(True)


class ReportImage(Handler):
    def get(self):
        source = db.get(self.request.get('img_id'))
        if source.picture:
            self.response.headers['Content-Type'] = 'image/png'
            self.response.out.write(source.picture)

class QrImage(Handler):
    def get(self):
        source = db.get(self.request.get('img_id'))
        if source.qr:
            self.response.headers['Content-Type'] = 'image/png'
            self.response.out.write(source.qr)

class MainHandler(Handler):
    def get(self):
        self.render('main.html')

def get_crime_tweets(update = False):
    key = 'crime_tweets'
    crimes = memcache.get(key)
    if crimes is None or update:
        logging.info("DB QUERY")
        logging.info("TWITTER API QUERY: iWitness")
        try:
            api = TwitterAPI(consumer_key, consumer_secret, access_token_key, access_token_secret)

            crime_list = api.request('search/tweets', {'q': 'theft OR syndicate OR loanshark OR harassment OR investigate OR investigation OR arrested OR robber OR robbery OR riot OR kidnap  from:SingaporePolice',
                                                       'count': 100,
                                                       'result_type': 'recent'})
            for tweet in crime_list:
                tweet_id = tweet['id_str']
                tweet_created = tweet['created_at']
                ts = to_datetime(tweet_created)
                duplicate = False
                for crime in get_crime_tweets():
                    if crime.tweet_id == tweet_id:
                        duplicate = True
                        break
                if not duplicate:
                    tweet_url = "https://twitter.com/SingaporePolice/statuses/" + tweet['id_str']
                    tweet_text = tweet['text']
                    new_crime = Crimes(parent=crime_id_key,
                                       tweet_id=tweet_id,
                                       content=tweet_text,
                                       url=tweet_url,
                                       posted=ts)
                    if 'media' in tweet['entities']:
                        picture_url = tweet['entities']['media'][0]['media_url_https']
                        new_crime.picture = picture_url
                    new_crime.put()
                    #get_crime_tweets(True)
        except:
            logging.error("No Internet Access")
        
        crimes = db.GqlQuery("SELECT * "
                             "FROM Crimes "
                             "WHERE ANCESTOR IS :1 "
                             "ORDER BY posted DESC",
                             crime_id_key)
        crimes = list(crimes)
        memcache.set(key, crimes)
    return crimes

class Crimewatch(Handler):
    def get(self):
        crime_list = get_crime_tweets()
        self.render('crime.html', crimes=crime_list)

class UpdateCrime(Handler):
    def get(self):
        get_crime_tweets(True)

def get_holidays(update = False):
    key = 'holidays'
    holidays = memcache.get(key)
    if holidays is None or update:
        holidays = db.GqlQuery("SELECT * "
                               "FROM Holidays "
                               "WHERE ANCESTOR IS :1 ",
                               holiday_key)
        holidays = list(holidays)
        memcache.set(key, holidays)
    return holidays

class SmsService(Handler):
    def verify_twilio(self, password, location):
        if len(password) == len(twilio_pass):
            if password == twilio_pass:
                return True
                #for obj in get_neighbourhood_list():
                #    if location == obj.neighbourhood:
                #        return True
        return False
        
    def get(self, password, location, uid):
        user_agent = self.request.headers['User-Agent']
        user = User.by_id(int(uid))
        location = location.replace('%20', ' ')
        location = location.replace('+', ' ')
        if user:
            if self.verify_twilio(password, location):
                try:
                    to = "+65%s" % user.phone
                    body = "One of your tags have been scanned at %s!" % location

                    if user_agent == "Custom-Android-App":
                        body = location
                    client = twilio.rest.TwilioRestClient(account_sid, auth_token)
                    message = client.messages.create(body=body,
                            to="+6591594775",
                            from_="+15743284027")
                    #print message.sid
                    logging.info("Message:", message.sid)
                except twilio.TwilioRestException as e:
                    #print e
                    logging.error("Twilio Exception:", e)
        self.error_404()

app = webapp2.WSGIApplication([
    ('/', MainHandler),
    ('/rain', RainLocation),
    ('/rain/map', RainMap),
    ('/rain/update', UpdateRainLocation),
    ('/haze', Haze),
    ('/haze/update', UpdateHaze),
    ('/signup', Register),
    ('/signin', Login),
    ('/logout', Logout),
    ('/lost/([a-z]+)/([0-9]+)(?:\.json)?(/[a-z]+)?', ReportPage),
    ('/lost/report', ReportLost),
    ('/lost(/[a-zA-Z]+)?(?:\.json)?', LostNFound),
    ('/admin/flagged/report', ReportViewer),
    ('/tree/arduino/([a-zA-Z0-9\s]+)/([0-9]+)/([0-9]+)/([0-9]+)/([0-9]+)', ArduinoData),
    ('/garden', CommunityTree),
    ('/happenings/([a-z]+)', Happenings),
    ('/happenings/twitter/update', UpdateTrending),
    ('/iwitness', Crimewatch),
    ('/iwitness/update', UpdateCrime),
    ('/img', ReportImage),
    ('/qr', QrImage),
    ('/services/twil/([a-zA-Z0-9]+)/([a-zA-Z0-9\s+.,_]+)/([0-9]+)', SmsService),
    ('/([a-zA-Z0-9_-]+)(/[a-z]+)?', Profile)
], debug=True)
