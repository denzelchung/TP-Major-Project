import os
import sys
import re
import random
import hashlib
import hmac
import logging
import time
import json
import math
import operator
import string
from string import letters
from PIL import Image
import datetime
from email.utils import parsedate_tz
import xml.dom.minidom as minidom

import cStringIO
import StringIO
import urllib
import urllib2
import webapp2
import jinja2
from google.appengine.ext import db
from google.appengine.api import images
from google.appengine.api import urlfetch
from google.appengine.ext import blobstore
from google.appengine.api import memcache
from google.appengine.api import files

import feedparser
from project_util import translate_html

sys.path.insert(0, 'libs')
from TwitterAPI import TwitterAPI
import twilio
import twilio.rest
import qrcode

# Twitter API auth
access_token_key = '429019953-hZ4Gag85ZEx4EilD44PYvlb8rS8dDQLRh2BwgtvC'
access_token_secret = 'BF4yNR9Gqrae6qPvCWkpOtLJlTRm82wjej4yPqjN4w74T'
consumer_key = '1YiVfFcb3qPWf2sIeMHKnnRAW'
consumer_secret = 'Q2OV6YA9oTZPsWyNqEHKeVcBZqEjHmFbw2oovsjGknKte6JLfi'

# Your Account Sid and Auth Token from twilio.com/user/account
account_sid = "AC39450644eb76e74e310bd841d2ed52aa"
auth_token  = "b29b9e93fece1d9b8590341bbec0bdb8"
twilio_pass = "YuwfE2fEBHqJDtEXF5"

secret = 'test'
rain_page = 'http://app2.nea.gov.sg/data/dt/xml/rainlocation_map.gif'

NEIGHBOURHOOD_LIST = ['Pasir Ris', 'Tampines', 'Simei', 'Test']

def split_dash(string):
    return string.split("-")

template_dir = os.path.join(os.path.dirname(__file__), 'templates')
jinja_env = jinja2.Environment(loader = jinja2.FileSystemLoader(template_dir),
                               autoescape = True, trim_blocks = True)
jinja_env.filters['split_dash'] = split_dash

missing_people_key = db.Key.from_path('Missing', 'people')
missing_pet_key = db.Key.from_path('Missing', 'pet')
missing_belonging_key = db.Key.from_path('Missing', 'belonging')
flag_report_key = db.Key.from_path('Flag', 'Report')
garden_key = db.Key.from_path('Garden', 'Status')
neighbourhood_key = db.Key.from_path('Neighbourhood', 'List')
rain_area_key = db.Key.from_path('Rain', 'Area')
crime_id_key = db.Key.from_path('Crime', 'ids')
holiday_key = db.Key.from_path('Holiday', '2014')

PAGE_SIZE = 5

def render_str(template, **params):
    t = jinja_env.get_template(template)
    return t.render(params)

def id_generator(size=27, chars=string.ascii_uppercase + string.ascii_lowercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))


def greetings_key(group = 'default'):
    return db.Key.from_path('greetings', group)

class GreetingDB(db.Model):
    message = db.StringProperty(required = True)

    @classmethod
    def by_id(cls, uid):
        return cls.get_by_id(uid, parent = greetings_key())


###### MEMCACHE #########

def get_garden_status(update = False):
    key = 'community_garden'
    garden_status = memcache.get(key)
    if garden_status is None or update:
        logging.error("DB QUERY")
        garden_status = db.GqlQuery("SELECT * "
                                    "FROM CommunityGarden "
                                    "WHERE ANCESTOR IS :1 "
                                    "ORDER BY points DESC ",
                                    garden_key)
        garden_status = list(garden_status)
        memcache.set(key, garden_status)
    return garden_status

def get_neighbourhood_list(update = False):
    key = 'neighbourhood_list'
    neighbourhood_list = memcache.get(key)
    if neighbourhood_list is None or update:
        logging.error("DB QUERY")
        neighbourhood_list = db.GqlQuery("SELECT * "
                                         "FROM Neighbourhoods "
                                         "WHERE ANCESTOR IS :1 ",
                                         neighbourhood_key)
        neighbourhood_list = list(neighbourhood_list)
        memcache.set(key, neighbourhood_list)
    return neighbourhood_list

def get_flagged_reports(update = False):
    key = 'flagged_report'
    reports = memcache.get(key)
    if reports is None or update:
        logging.error("DB QUERY")
        reports = db.GqlQuery("SELECT * "
                              "FROM FlagReport "
                              "WHERE ANCESTOR IS :1 ",
                              flag_report_key)
        reports = list(reports)
        memcache.set(key, reports)
    return reports
#########################
def to_datetime(datestring):
    time_tuple = parsedate_tz(datestring.strip())
    dt = datetime.datetime(*time_tuple[:6])
    return dt - datetime.timedelta(hours=-8, seconds=time_tuple[-1])

class Crimes(db.Model):
    tweet_id = db.StringProperty(required = True)
    content = db.TextProperty(required = True)
    picture = db.LinkProperty()
    url = db.LinkProperty(required = True)
    posted = db.DateTimeProperty(required = True)


# Code for retrieving and parsing RSS feeds
def process(url):
    """
    Fetches news items from the rss url and parses them.
    Returns a list of NewsStory-s.
    """
    feed = feedparser.parse(url)
    entries = feed.entries
    ret = []
    for entry in entries:
        guid = entry.guid
        title = translate_html(entry.title)
        summary = translate_html(entry.summary)
        logging.info(summary)
        summary = entry.summary
        link = entry.link
        updated = entry.updated
        #try:
        #    subject = translate_html(entry.tags[0]['term'])
        #except AttributeError:
        #    subject = ""
        newsStory = NewsStory(guid, title, summary, link, updated)
        ret.append(newsStory)
    return ret

class NewsStory(object):
    def __init__(self, guid, title, summary, link, updated):
        self.guid = guid
        self.title = title
        self.summary = summary
        self.link = link
        self.updated = updated

    def getGuid(self):
        return self.guid

    def getTitle(self):
        return self.title

    def getSummary(self):
        return self.summary

    def getLink(self):
        return self.link

    def getUpdated(self):
        return self.updated

def remove_html(raw_html):
    clean_re = re.compile('<.*?>')
    text = re.sub(clean_re, '', raw_html)
    return text

# User
def is_empty(field):
    return field == ""

def make_salt(length = 5):
    return ''.join(random.choice(letters) for x in xrange(length))

def make_pw_hash(name, pw, salt = None):
    if not salt:
        salt = make_salt()
    h = hashlib.sha256(name + pw + salt).hexdigest()
    return '%s,%s' % (salt, h)

def valid_pw(name, password, h):
    salt = h.split(',')[0]
    return h == make_pw_hash(name, password, salt)

# min 3, max 20
# a-z, A-Z, 0-9
USER_RE = re.compile(r"^[a-zA-Z0-9_-]{3,20}$")
def valid_username(username):
    return username and USER_RE.match(username)

# min 3, max 20
PASS_RE = re.compile(r"^.{3,20}$")
def valid_password(password):
    return password and PASS_RE.match(password)

# ___@___.___
EMAIL_RE  = re.compile(r'^[\S]+@[\S]+\.[\S]+$')
def valid_email(email):
    return email and EMAIL_RE.match(email)

CONTACT_RE = re.compile(r'^[0-9]{8}')
def valid_contact(contact):
    return contact and CONTACT_RE.match(contact)

def users_key(group = 'default'):
    return db.Key.from_path('users', group)

class User(db.Model):
    username = db.StringProperty(required = True)
    password = db.StringProperty(required = True)
    email = db.StringProperty(required = True)
    phone = db.StringProperty(required = True)
    gender = db.StringProperty(required = True)
    location = db.StringProperty()
    picture = db.BlobProperty()
    qr = db.BlobProperty()
    joinDate = db.DateTimeProperty(auto_now_add = True)
    lastLogin = db.DateTimeProperty(auto_now = True)

    @classmethod
    def by_id(cls, uid):
        user = cls.get_by_id(uid, parent = users_key())
        if user is None:
            user = cls.get_by_id(uid, parent = users_key("admin"))
        return user

    @classmethod
    def get_user_type(cls, uid):
        user = cls.get_by_id(uid, parent = users_key())
        if user:
            return "default"
        user = cls.get_by_id(uid, parent = users_key("admin"))
        if user:
            return "admin"
        return user

    @classmethod
    def by_name(cls, name):
        u = cls.all().filter('username =', name).get()
        return u

    @classmethod
    def by_email(cls, email):
        u = cls.all().filter('email =', email).get()
        return u

    @classmethod
    def register(cls, name, pw, email, phone, gender, location, photo):
        pw_hash = make_pw_hash(name, pw)
        new_user = cls(parent=users_key(),
                       username=name,
                       password=pw_hash,
                       email=email,
                       phone=phone,
                       gender=gender,
                       location=location)
        if photo:
            photo = images.resize(photo, width=250, height=300)
            new_user.picture = db.Blob(photo)
        
        return new_user

    @classmethod
    def login(cls, name, pw):
        u = cls.by_name(name)
        if u and valid_pw(name, pw, u.password):
            u.put()
            return u
    

class RainArea(db.Model):
    area = db.StringProperty(required = True)
    isRaining = db.BooleanProperty(required = True)
    rainStatus = db.StringProperty()
    loaded = db.DateTimeProperty(auto_now_add = True)

class Holidays(db.Model):
    name = db.StringProperty(required = True)
    day = db.StringProperty(required = True)
    date = db.DateTimeProperty(required = True)

class Neighbourhoods(db.Model):
    neighbourhood = db.StringProperty(required = True)

class CommunityGarden(db.Model):
    #neighbourhood = db.StringProperty(required = True)
    neighbourhood = db.ReferenceProperty(Neighbourhoods)
    moistureLevel = db.IntegerProperty(required = True)
    temperature = db.IntegerProperty(required = True)
    humidity = db.IntegerProperty(required = True)
    heatIndex = db.IntegerProperty(required = True)
    points = db.IntegerProperty(required = True, default=0)
    avg_moisture = db.FloatProperty()
    avg_temperature = db.FloatProperty()
    avg_humidity = db.FloatProperty()
    avg_heatIndex = db.FloatProperty()
    avg_num = db.IntegerProperty()
    dry = db.BooleanProperty(default=False)
    updated = db.DateTimeProperty(auto_now = True)

    def update(self, moistureLevel=None, temperature=None,
               humidity=None, heatIndex=None, points=None):
        if moistureLevel is not None:
            if moistureLevel <= self.moistureLevel and \
               moistureLevel < 100:
                self.dry = True
            else:
                self.dry = False
            self.moistureLevel = moistureLevel
        if temperature is not None:
            self.temperature = temperature
        if humidity is not None:
            self.humidity = humidity
        if heatIndex is not None:
            self.heatIndex = heatIndex
        self.put()

    def updateAvg(self, newMoisture, newTemperature,
                  newHumidity, newHeatIndex):
        oldMoistureAvg = self.avg_moisture
        oldTemperatureAvg = self.avg_temperature
        oldHumidityAvg = self.avg_humidity
        oldHeatIndexAvg = self.avg_heatIndex

        self.avg_moisture = ((self.avg_moisture * self.avg_num)
                             + newMoisture) / (self.avg_num + 1)
        self.avg_temperature = ((self.avg_temperature * self.avg_num)
                             + newTemperature) / (self.avg_num + 1)
        self.avg_humidity = ((self.avg_humidity * self.avg_num)
                             + newHumidity) / (self.avg_num + 1)
        self.avg_heatIndex = ((self.avg_heatIndex * self.avg_num)
                             + newHeatIndex) / (self.avg_num + 1)
        
        if newMoisture > 200 and newMoisture < 600:
            self.points += int(10 * (newMoisture / self.avg_moisture))
        if newTemperature > 25 and newTemperature < 32:
            self.points += int(10 * (newTemperature / self.avg_temperature))
        if newHumidity > 50 and newHumidity < 90:
            self.points += int(10 * (newHumidity / self.avg_humidity))
        
        self.put()

class FlagReport(db.Model):
    reportId = db.IntegerProperty(required = True)
    reportReason = db.StringProperty(required = True)
    reasonOther = db.StringProperty()
    reportTime = db.DateTimeProperty(auto_now_add = True)

class Missing(db.Model):
    name = db.StringProperty(required = True)
    picture = db.BlobProperty()
    lastSeenAt = db.StringProperty(required = True)
    lastSeen = db.StringProperty(required = True)
    contactDetails = db.StringProperty(required = True)
    additionalDetails = db.TextProperty()
    user = db.StringProperty(required = True)
    found = db.BooleanProperty(required = True, default=False)
    flagged = db.BooleanProperty(required = True, default=False)
    created = db.DateTimeProperty(auto_now_add = True)
    lastModified = db.DateTimeProperty(auto_now = True)

    def update(self, name=None, picture=None, lastSeenAt=None,
               lastSeen=None, contactDetails=None,
               additionalDetails=None):
        if name is not None:
            self.name = name
        if picture is not None:
            self.picture = picture
        if lastSeenAt is not None:
            self.lastSeenAt = lastSeenAt
        if lastSeen is not None:
            self.lastSeen = lastSeen
        if contactDetails is not None:
            self.contactDetails = contactDetails
        if additionalDetails is not None:
            self.additionalDetails = additionalDetails
        self.put()

    def mark_as_found(self):
        self.found = not self.found
        self.put()

    def mark_flagged(self):
        self.flagged = True
        self.put()

    def clear_flag(self):
        self.flagged = False
        self.put()

    def render(self, user, userType, missing_type):
        self.currentUser = user
        self.userType = userType
        self.type = missing_type.lower()
        if self.picture:
            self.photo = "/img?img_id=%s" % self.key()
            logging.info(dir(self.photo))
        else:
            self.photo = None
        self.created = self.created + datetime.timedelta(hours = 8)
        self.lastModified = self.lastModified + datetime.timedelta(hours = 8)
        date, time = self.lastSeen.split(" ")
        year, month, day = map(int, date.split("-"))
        hour, minute, second = map(int, time.split(":"))
        self.lastSeenTime = datetime.datetime(year, month, day, hour, minute, second)
        return render_str("missing.html", report = self)
        

    def as_dict(self):
        self.photo = None
        if self.picture:
            self.photo = "/img?img_id=%s" % self.key()
        self.created = self.created + datetime.timedelta(hours = 8)
        self.lastModified = self.lastModified + datetime.timedelta(hours = 8)
        date, time = self.lastSeen.split(" ")
        year, month, day = map(int, date.split("-"))
        hour, minute, second = map(int, time.split(":"))
        self.lastSeenTime = datetime.datetime(year, month, day, hour, minute, second)

        d = {'name': self.name,
             'last_seen': self.lastSeenAt,
             'last_seen_date': self.lastSeenTime.strftime("%d %b %Y"),
             'last_seen_time': self.lastSeenTime.strftime("%I:%M %p"),
             'contact_details': self.contactDetails,
             'user': self.user,
             'found': self.found,
             'posted_date': self.created.strftime("%b %d, %Y"),
             'posted_time': self.created.strftime("%I:%M %p")
             }
        if self.photo:
            d['picture'] = self.photo
        if self.additionalDetails:
            d['additional_details'] = self.additionalDetails
        if not (self.created.strftime("%I:%M %p") ==
                self.lastModified.strftime("%I:%M %p") and
                (self.created.strftime("%b %d, %Y") ==
                 self.lastModified.strftime("%b %d, %Y"))):
             d['last_modified_date'] = self.lastModified.strftime("%b %d, %Y")
             d['last_modified_time'] = self.lastModified.strftime("%I:%M %p")
        
        return d

    
class MissingPerson(Missing):
    age = db.StringProperty(required = True)

    def update(self, name=None, age=None, picture=None,
               lastSeenAt=None, lastSeen=None,
               contactDetails=None, additionalDetails=None):
        if name is not None:
            self.name = name
        if age is not None:
            self.age = age
        if picture is not None:
            self.picture = picture
        if lastSeenAt is not None:
            self.lastSeenAt = lastSeenAt
        if lastSeen is not None:
            self.lastSeen = lastSeen
        if contactDetails is not None:
            self.contactDetails = contactDetails
        if additionalDetails is not None:
            self.additionalDetails = additionalDetails
        self.put()

    def as_dict(self):
        self.photo = None
        if self.picture:
            self.photo = "/img?img_id=%s" % self.key()
        self.created = self.created + datetime.timedelta(hours = 8)
        self.lastModified = self.lastModified + datetime.timedelta(hours = 8)
        date, time = self.lastSeen.split(" ")
        year, month, day = map(int, date.split("-"))
        hour, minute, second = map(int, time.split(":"))
        self.lastSeenTime = datetime.datetime(year, month, day, hour, minute, second)

        d = {'name': self.name,
             'age': self.age,
             'last_seen': self.lastSeenAt,
             'last_seen_date': self.lastSeenTime.strftime("%d %b %Y"),
             'last_seen_time': self.lastSeenTime.strftime("%I:%M %p"),
             'contact_details': self.contactDetails,
             'user': self.user,
             'found': self.found,
             'posted_date': self.created.strftime("%b %d, %Y"),
             'posted_time': self.created.strftime("%I:%M %p")
             }
        if self.photo:
            d['picture'] = self.photo
        if self.additionalDetails:
            d['additional_details'] = self.additionalDetails
        if not (self.created.strftime("%I:%M %p") ==
                self.lastModified.strftime("%I:%M %p") and
                (self.created.strftime("%b %d, %Y") ==
                 self.lastModified.strftime("%b %d, %Y"))):
             d['last_modified_date'] = self.lastModified.strftime("%b %d, %Y")
             d['last_modified_time'] = self.lastModified.strftime("%I:%M %p")
        
        return d

class MissingPet(Missing):
    pass

class MissingBelonging(Missing):
    pass


# rain intensities
heavy_rain = [119, 143]
moderate_rain = [173, 192, 205, 217, 225, 232, 179, 149, 144]
light_rain = [83, 91, 130, 167, 178]

def isRaining(area1, area2):
    raining = False
    rain_status = ""
    for color in area1.getcolors():
        if color[1] == 124:
            ground = color[0]
            break
    for color in area2.getcolors():
        if color[1] in heavy_rain:
            #logging.info(color[1])
            return (True, "heavy")
        elif color[1] in moderate_rain:
            raining = True
            rain_status = "moderate"
        elif color[1] in light_rain and rain_status != "moderate":
            raining = True
            rain_status = "light"
        # check if the land area is the same
        elif color[1] == 124 and rain_status != "moderate":
            raining = ground != color[0]
            if raining:
                rain_status = "light"
        
    return (raining, rain_status)

def segment_rain_img(im):
    # check if "Radar Under Maintenance"
    if max(im.convert('L').getcolors())[1] == 255:
        return 'Radar Under Maintenance'

    # Open and crop/segment map
    # Convert to greyscale ('L')
    # Check if raining
    original_no_rain = Image.open("no_rain.gif")
    ang_mo_kio_box = (281, 151, 338, 187)
    bedok_box = (402, 225, 486, 279)
    bishan_box = (280, 187, 338, 226)
    boon_lay_box = (97, 197, 157, 234)
    bukit_timah_box = (208, 196, 281, 246)
    changi_box = (486, 145, 569, 270)
    chinatown_box = (289, 293, 326, 314)
    choa_chu_kang_box = (165, 157, 226, 196)
    city_hall_box = (305, 267, 340, 292)
    clementi_box = (157, 234, 223, 293)
    hougang_box = (338, 170, 402, 206)
    jurong_east_box = (157, 197, 208, 234)
    jurong_ind_box = (97, 234, 157, 282)
    jurong_island_box = (54, 281, 178, 379)
    lim_chu_kang_box = (85, 78, 166, 197)
    macpherson_box = (338, 207, 402, 242)
    marina_south_box = (321, 292, 348, 324)
    orchard_box = (272, 267, 305, 292)
    pasir_ris_box = (424, 161, 486, 196)
    paya_lebar_box = (338, 242, 402, 288)
    pulau_tekong_box = (532, 89, 631, 210)
    pulau_ubin_box = (419, 101, 510, 142)
    punggol_box = (338, 108, 417, 137)
    queenstown_box = (222, 246, 281, 293)
    redhill_box = (237, 293, 290, 327)
    sembawang_box = (265, 41, 323, 84)
    sengkang_box = (338, 137, 417, 170)
    sentosa_box = (261, 328, 314, 358)
    tampines_box = (424, 196, 486, 238)
    toa_payoh_box = (280, 226, 338, 267)
    tuas_view_box = (8, 203, 97, 294)
    west_coast_box = (185, 293, 238, 328)
    woodlands_box = (199, 58, 265, 111)
    yew_tee_box = (166, 123, 189, 157)
    yishun_box = (265, 84, 323, 125)
    
    ang_mo_kio_orig = original_no_rain.crop(ang_mo_kio_box).convert('L')
    bedok_orig = original_no_rain.crop(bedok_box).convert('L')
    bishan_orig = original_no_rain.crop(bishan_box).convert('L')
    boon_lay_orig = original_no_rain.crop(boon_lay_box).convert('L')
    bukit_timah_orig = original_no_rain.crop(bukit_timah_box).convert('L')
    changi_orig = original_no_rain.crop(changi_box).convert('L')
    chinatown_orig = original_no_rain.crop(chinatown_box).convert('L')
    choa_chu_kang_orig = original_no_rain.crop(choa_chu_kang_box).convert('L')
    city_hall_orig = original_no_rain.crop(city_hall_box).convert('L')
    clementi_orig = original_no_rain.crop(clementi_box).convert('L')
    hougang_orig = original_no_rain.crop(hougang_box).convert('L')
    jurong_east_orig = original_no_rain.crop(jurong_east_box).convert('L')
    jurong_ind_orig = original_no_rain.crop(jurong_ind_box).convert('L')
    jurong_island_orig = original_no_rain.crop(jurong_island_box).convert('L')
    lim_chu_kang_orig = original_no_rain.crop(lim_chu_kang_box).convert('L')
    macpherson_orig = original_no_rain.crop(macpherson_box).convert('L')
    marina_south_orig = original_no_rain.crop(marina_south_box).convert('L')
    orchard_orig = original_no_rain.crop(orchard_box).convert('L')
    pasir_ris_orig = original_no_rain.crop(pasir_ris_box).convert('L')
    paya_lebar_orig = original_no_rain.crop(paya_lebar_box).convert('L')
    pulau_tekong_orig = original_no_rain.crop(pulau_tekong_box).convert('L')
    pulau_ubin_orig = original_no_rain.crop(pulau_ubin_box).convert('L')
    punggol_orig = original_no_rain.crop(punggol_box).convert('L')
    queenstown_orig = original_no_rain.crop(queenstown_box).convert('L')
    redhill_orig = original_no_rain.crop(redhill_box).convert('L')
    sembawang_orig = original_no_rain.crop(sembawang_box).convert('L')
    sengkang_orig = original_no_rain.crop(sengkang_box).convert('L')
    sentosa_orig = original_no_rain.crop(sentosa_box).convert('L')
    tampines_orig = original_no_rain.crop(tampines_box).convert('L')
    toa_payoh_orig = original_no_rain.crop(toa_payoh_box).convert('L')
    tuas_view_orig = original_no_rain.crop(tuas_view_box).convert('L')
    west_coast_orig = original_no_rain.crop(west_coast_box).convert('L')
    woodlands_orig = original_no_rain.crop(woodlands_box).convert('L')
    yew_tee_orig = original_no_rain.crop(yew_tee_box).convert('L')
    yishun_orig = original_no_rain.crop(yishun_box).convert('L')

    ang_mo_kio_img = im.crop(ang_mo_kio_box).convert('L')
    bedok_img = im.crop(bedok_box).convert('L')
    bishan_img = im.crop(bishan_box).convert('L')
    boon_lay_img = im.crop(boon_lay_box).convert('L')
    bukit_timah_img = im.crop(bukit_timah_box).convert('L')
    changi_img = im.crop(changi_box).convert('L')
    chinatown_img = im.crop(chinatown_box).convert('L')
    choa_chu_kang_img = im.crop(choa_chu_kang_box).convert('L')
    city_hall_img = im.crop(city_hall_box).convert('L')
    clementi_img = im.crop(clementi_box).convert('L')
    hougang_img = im.crop(hougang_box).convert('L')
    jurong_east_img = im.crop(jurong_east_box).convert('L')
    jurong_ind_img = im.crop(jurong_ind_box).convert('L')
    jurong_island_img = im.crop(jurong_island_box).convert('L')
    lim_chu_kang_img = im.crop(lim_chu_kang_box).convert('L')
    macpherson_img = im.crop(macpherson_box).convert('L')
    marina_south_img = im.crop(marina_south_box).convert('L')
    orchard_img = im.crop(orchard_box).convert('L')
    pasir_ris_img = im.crop(pasir_ris_box).convert('L')
    paya_lebar_img = im.crop(paya_lebar_box).convert('L')
    pulau_tekong_img = im.crop(pulau_tekong_box).convert('L')
    pulau_ubin_img = im.crop(pulau_ubin_box).convert('L')
    punggol_img = im.crop(punggol_box).convert('L')
    queenstown_img = im.crop(queenstown_box).convert('L')
    redhill_img = im.crop(redhill_box).convert('L')
    sembawang_img = im.crop(sembawang_box).convert('L')
    sengkang_img = im.crop(sengkang_box).convert('L')
    sentosa_img = im.crop(sentosa_box).convert('L')
    tampines_img = im.crop(tampines_box).convert('L')
    toa_payoh_img = im.crop(toa_payoh_box).convert('L')
    tuas_view_img = im.crop(tuas_view_box).convert('L')
    west_coast_img = im.crop(west_coast_box).convert('L')
    woodlands_img = im.crop(woodlands_box).convert('L')
    yew_tee_img = im.crop(yew_tee_box).convert('L')
    yishun_img = im.crop(yishun_box).convert('L')

    areas = {"Ang Mo Kio": (ang_mo_kio_orig, ang_mo_kio_img),
             "Bedok": (bedok_orig, bedok_img),
             "Bishan": (bishan_orig, bishan_img),
             "Boon Lay": (boon_lay_orig, boon_lay_img),
             "Bukit Timah": (bukit_timah_orig, bukit_timah_img),
             "Changi": (changi_orig, changi_img),
             "Chinatown": (chinatown_orig, chinatown_img),
             "Choa Chu Kang": (choa_chu_kang_orig, choa_chu_kang_img),
             "City Hall": (city_hall_orig, city_hall_img),
             "Clementi": (clementi_orig, clementi_img),
             "Hougang": (hougang_orig, hougang_img),
             "Jurong East": (jurong_east_orig, jurong_east_img),
             "Jurong Industrial Estate": (jurong_ind_orig, jurong_ind_img),
             "Jurong Island": (jurong_island_orig, jurong_island_img),
             "Lim Chu Kang": (lim_chu_kang_orig, lim_chu_kang_img),
             "MacPherson": (macpherson_orig, macpherson_img),
             "Marina South": (marina_south_orig, marina_south_img),
             "Orchard": (orchard_orig, orchard_img),
             "Pasir Ris": (pasir_ris_orig, pasir_ris_img),
             "Paya Lebar": (paya_lebar_orig, paya_lebar_img),
             "Pulau Tekong": (pulau_tekong_orig, pulau_tekong_img),
             "Pulau Ubin": (pulau_ubin_orig, pulau_ubin_img),
             "Punggol": (punggol_orig, punggol_img),
             "Queenstown": (queenstown_orig, queenstown_img),
             "Redhill": (redhill_orig, redhill_img),
             "Sembawang": (sembawang_orig, sembawang_img),
             "Sengkang": (sengkang_orig, sengkang_img),
             "Sentosa": (sentosa_orig, sentosa_img),
             "Tampines": (tampines_orig, tampines_img),
             "Toa Payoh": (toa_payoh_orig, toa_payoh_img),
             "Tuas View": (tuas_view_orig, tuas_view_img),
             "West Coast": (west_coast_orig, west_coast_img),
             "Woodlands": (woodlands_orig, woodlands_img),
             "Yew Tee": (yew_tee_orig, yew_tee_img),
             "Yishun": (yishun_orig, yishun_img)}
    rain_area = []
    not_rain_area = []
    # clear database
    db.delete(RainArea.all())

    for area in areas:
        img_orig, img_realtime = areas[area]
        rain_status = isRaining(img_orig, img_realtime)
        # True if raining, False otherwise
        if rain_status[0]:
            #rain_area.append(area)
            RainArea(parent=rain_area_key,
                     area=area,
                     isRaining=True,
                     rainStatus=rain_status[1]).put()
        else:
            #not_rain_area.append(area)
            RainArea(parent=rain_area_key,
                     area=area,
                     isRaining=False).put()


# Refresh rain area every 2 minute
def refresh_rain_areas(areas, im):
    if not areas.get() or \
       (datetime.datetime.now().minute - areas[0].loaded.minute) >= 2 or \
       (datetime.datetime.now().hour != areas[0].loaded.hour):
        return segment_rain_img(im)


def getLastModified(report):
    return report[0].lastModified

def getCreated(report):
    return report[0].created
