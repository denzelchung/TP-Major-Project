"""API implemented using Google Cloud Endpoints.

Defined here are the ProtoRPC messages needed to define Schemas for methods
as well as those methods defined in an API.
"""

import endpoints
from protorpc import messages
from protorpc import message_types
from protorpc import remote

from database import *

package = 'Hello'


class RainLocation(messages.Message):
    location = messages.StringField(1, required=True)
    isRaining = messages.BooleanField(2, required=True)
    rainStatus = messages.StringField(3)

class RainLocationCollection(messages.Message):
    locations = messages.MessageField(RainLocation, 1, repeated=True)

class LoginUser(messages.Message):
    username = messages.StringField(1, required=True)
    email = messages.StringField(2, required=True)
    phone = messages.StringField(3, required=True)
    gender = messages.StringField(4, required=True)
    location = messages.StringField(5)
    picture = messages.StringField(6)
    joinDate = messages.StringField(7, required=True)

class ValidUser(messages.Message):
    username_error = messages.StringField(1, required=True)
    password_error = messages.StringField(2, required=True)
    email_error = messages.StringField(3, required=True)

class GardenStatus(messages.Message):
    neighbourhood = messages.StringField(1, required=True)
    moistureLevel = messages.IntegerField(2, required=True)
    temperature = messages.FloatField(3, required=True)
    humidity = messages.FloatField(4, required=True)
    heatIndex = messages.FloatField(5, required=True)

class GardenUpdate(messages.Message):
    updated = messages.BooleanField(1, required=True)

class GardenRecord(messages.Message):
    moistureLevel = messages.IntegerField(1, required=True)
    temperature = messages.IntegerField(2, required=True)
    humidity = messages.IntegerField(3, required=True)
    heatIndex = messages.IntegerField(4, required=True)

class SingleNeighbourhood(messages.Message):
    neighbourhood = messages.StringField(1, required=True)
    points = messages.IntegerField(2, required=True)

class NeighbourhoodCollection(messages.Message):
    neighbourhoods = messages.MessageField(SingleNeighbourhood, 1, repeated=True)


@endpoints.api(name='cenplusplus', version='v1')
class Api(remote.Service):
    """API v1."""

    """Rain Areas API"""
    @endpoints.method(message_types.VoidMessage, RainLocationCollection,
                      path='rainAreas', http_method='GET',
                      name='rain.listAreas')
    def locations_list(self, unused_request):
        STORED_LOCATIONS = []
        areas = RainArea.all().order("area")

        rain_picture = db.Blob(urlfetch.Fetch(rain_page).content)
        if rain_picture:
            img_file = cStringIO.StringIO(urllib.urlopen(rain_page).read())
            im = Image.open(img_file)
            refresh_rain_areas(areas, im)
##            if not areas.get() or \
##               (datetime.datetime.now().minute - areas[0].loaded.minute) >= 5 or \
##               (datetime.datetime.now().hour != areas[0].loaded.hour):
##                segment_rain_img(im)
                #areas = RainArea.all().order("-area")
            for area in areas:
                STORED_LOCATIONS.append(RainLocation(location=area.area,
                                                     isRaining=area.isRaining,
                                                     rainStatus=area.rainStatus))
        return RainLocationCollection(locations=STORED_LOCATIONS)


    ID_RESOURCE = endpoints.ResourceContainer(
            message_types.VoidMessage,
            id=messages.StringField(1, variant=messages.Variant.STRING))

    @endpoints.method(ID_RESOURCE, RainLocation,
                      path='rainAreas/{id}', http_method='GET',
                      name='rain.getArea')
    def location_get(self, request):
        STORED_LOCATIONS = []
        try:
            area = RainArea.all().filter("area = ", str(request.id))
            rain_picture = db.Blob(urlfetch.Fetch(rain_page).content)
            if rain_picture:
                img_file = cStringIO.StringIO(urllib.urlopen(rain_page).read())
                im = Image.open(img_file)
                refresh_rain_areas(area, im)
##                if not area or \
##                   (datetime.datetime.now().minute - area[0].loaded.minute) >= 5 or \
##                   (datetime.datetime.now().hour != area[0].loaded.hour):
##                    segment_rain_img(im)
                    #area = RainArea.all().filter("area = ", str(request.id))
                if area:
                    area = area.fetch(1)
                    return RainLocation(location=area[0].area,
                                        isRaining=area[0].isRaining,
                                        rainStatus=area[0].rainStatus)
                else:
                    raise endpoints.NotFoundException('Area "%s" not found.' %
                                                      (request.id,))
        except:
            raise endpoints.NotFoundException('Area "%s" not found.' %
                                                  (request.id,))

    """Login API"""
    LOGIN_RESOURCE = endpoints.ResourceContainer(
            message_types.VoidMessage,
            username=messages.StringField(1, variant=messages.Variant.STRING, required=True),
            password=messages.StringField(2, variant=messages.Variant.STRING, required=True))

    @endpoints.method(LOGIN_RESOURCE, LoginUser,
                      path='login/{username}', http_method='POST',
                      name='login.validate')
    def validate_login(self, request):
        u = User.login(str(request.username), str(request.password))
        if u:
            photo = ''
            if u.picture:
                photo = "/img?img_id=%s" % u.key()
                
            return LoginUser(username=u.username,
                             email=u.email,
                             phone=u.phone,
                             gender=u.gender,
                             location=u.location,
                             picture=photo,
                             joinDate=str(u.joinDate))
        else:
            raise endpoints.BadRequestException('Invalid login')

    """Register API"""
##    REGISTER_RESOURCE = endpoints.ResourceContainer(
##            message_types.VoidMessage,
##            username=messages.StringField(1, variant=messages.Variant.STRING, required=True),
##            password=messages.StringField(2, variant=messages.Variant.STRING, required=True),
##            email=messages.StringField(3, variant=messages.Variant.STRING, required=True),
##            phone=messages.StringField(4, variant=messages.Variant.STRING, required=True),
##            gender=messages.StringField(5, variant=messages.Variant.STRING, required=True),
##            location=messages.StringField(6, variant=messages.Variant.STRING, required=True),
##            picture=messages.BytesField(7, variant=messages.Variant.BYTES)
##            )
##
##    @endpoints.method(REGISTER_RESOURCE, LoginUser,
##                      path='register', http_method='POST',
##                      name='login.register')
##    def validate_register(self, request):
####        params = dict(username = request.username,
####                      email = request.email,
####                      phone = request.phone,
####                      gender = request.gender,
####                      location = request.location)
##        have_error = False
##        if not valid_username(request.username):
##            #params['error_username'] = "Invalid username"
##            have_error = True
##        if not valid_password(request.password):
##            #params['error_password'] = "Invalid password"
##            have_error = True
##        if not valid_email(request.email):
##            #params['error_email'] = "Invalid email"
##            have_error = True
##        if not valid_contact(request.phone):
##            #params['error_phone'] = "Invalid number"
##            have_error = True
##
##        if not have_error:
##            u = User.by_name(request.username)
##            if not u:
##                u = User.by_email(request.email)
##                if not u:
##                    u = User.register(request.username,
##                                      request.password,
##                                      request.email,
##                                      request.phone,
##                                      request.gender,
##                                      request.location,
##                                      request.picture)
##                    u.put()
##                    return LoginUser(username=u.username,
##                                     email=u.email,
##                                     phone=u.phone,
##                                     gender=u.gender,
##                                     location=u.location,
##                                     picture=u.picture,
##                                     joinDate=str(u.joinDate))
##                else:
##                    params['error_email'] = 'Email Address Already in Use'
##                    raise endpoints.BadRequestException('Email Address Already in Use')
##            else:
##                params['error_username'] = 'Username is Taken'
##                raise endpoints.BadRequestException('Username is Taken')
##        else:
##            raise endpoints.BadRequestException('')

    REGISTER_RESOURCE = endpoints.ResourceContainer(
            message_types.VoidMessage,
            username=messages.StringField(1, variant=messages.Variant.STRING, required=True),
            password=messages.StringField(2, variant=messages.Variant.STRING, required=True),
            email=messages.StringField(3, variant=messages.Variant.STRING, required=True)
            )
    @endpoints.method(REGISTER_RESOURCE, ValidUser,
                      path='register', http_method='POST',
                      name='login.register')
    def validate_register(self, request):
        validUsername = ''
        validPassword = ''
        validEmail = ''
        
        u = User.by_name(request.username)
        if u:
            validUsername = 'Username is Taken'
        elif not valid_username(request.username):
            validUsername = 'Invalid username'
            
        if not valid_password(request.password):
            validPassword = 'Invalid password'

        u = User.by_email(request.email)
        if u:
            validEmail = 'Email Address Already in Use'
        elif not valid_email(request.email):
            validEmail = 'Invalid email'

        return ValidUser(username_error=validUsername,
                         password_error=validPassword,
                         email_error=validEmail)


##    REPORT_RESOURCE = endpoints.ResourceContainer(
##            message_types.VoidMessage,
##            name=messages.StringField(1, variant=messages.Variant.STRING, required=True),
##            age=messages.IntegerField(2, variant=messages.Variant.INT32, required=True),
##            picture=messages.BytesField(3, variant=messages.Variant.BYTES),
##            last_seen_at=messages.StringField(4, variant=messages.Variant.STRING, required=True),
##            last_seen=message_types.DateTimeField(5, variant=messages.Variant.MESSAGE, required=True),
##            contact=messages.StringField(6, variant=messages.Variant.STRING, required=True),
##            additional_details=messages.StringField(7, variant=messages.Variant.STRING, required=True)
##            )
##
##    @endpoints.method(REPORT_RESOURCE, LoginUser,
##                      path='report', http_method='POST',
##                      name='report.people')
##    def report_people(self, request):
##        if is_empty(request.name) or \
##           is_empty(request.last_seen_at) or \
##           is_empty(request.contact):
##            raise endpoints.BadRequestException('')


##    GARDEN_RESOURCE = endpoints.ResourceContainer(
##            message_types.VoidMessage,
##            neighbourhood=messages.StringField(1, variant=messages.Variant.STRING, required=True),
##            moistureLevel=messages.IntegerField(2, variant=messages.Variant.INT32, required=True),
##            temperature=messages.FloatField(3, variant=messages.Variant.FLOAT, required=True),
##            humidity=messages.FloatField(4, variant=messages.Variant.FLOAT, required=True),
##            heatIndex=messages.FloatField(5, variant=messages.Variant.FLOAT, required=True)
##            )
##    @endpoints.method(GARDEN_RESOURCE, GardenUpdate,
##                      path='garden', http_method='GET',
##                      name='garden.update')
##    def garden_status(self, request):
##        updated = False
##        neighbourhood = request.neighbourhood
##        for obj in get_neighbourhood_list():
##            if neighbourhood == obj.neighbourhood:
##                newMoistureLevel = request.moistureLevel
##                newTemperature = request.temperature
##                newHumidity = request.humidity
##                newHeatIndex = request.heatIndex
##                q = db.GqlQuery("SELECT * "
##                                "FROM CommunityGarden "
##                                "WHERE ANCESTOR IS :1 "
##                                "AND neighbourhood = :2",
##                                garden_key,
##                                obj)
##                garden = q.get()
##                
##                if garden:
##                    if garden.neighbourhood.neighbourhood == neighbourhood:
##                        points = garden.points
##                        moistureLevel = garden.moistureLevel
##                        temperature = garden.temperature
##                        humidity = garden.humidity
##                        heatIndex = garden.heatIndex
##
##                        updateMoisture = None
##                        updateTemperature = None
##                        updateHumidity = None
##                        updateHeatIndex = None
##                            
##                        if moistureLevel != newMoistureLevel:
##                            updateMoisture = newMoistureLevel
##                        if temperature != newTemperature:
##                            updateTemperature = newTemperature
##                        if humidity != newHumidity:
##                            updateHumidity = newHumidity
##                        if heatIndex != newHeatIndex:
##                            updateHeatIndex = newHeatIndex
##                                
##                        garden.update(moistureLevel = updateMoisture,
##                                      temperature = updateTemperature,
##                                      humidity = updateHumidity,
##                                      heatIndex = updateHeatIndex)
##                        updated = True
##                        get_garden_status(True)
##                else:
##                    # Create new neighbourhood
##                    CommunityGarden(parent=garden_key, neighbourhood=obj,
##                                    moistureLevel=newMoistureLevel,
##                                    temperature=newTemperature,
##                                    humidity=newHumidity,
##                                    heatIndex=newHeatIndex,
##                                    points=0).put()
##                    updated = True
##        return GardenUpdate(updated=updated)
##            # TODO: update points
##            # TODO: update db

    GARDEN_RESOURCE = endpoints.ResourceContainer(
            message_types.VoidMessage,
            location=messages.StringField(1, variant=messages.Variant.STRING, required=True),
            )
    @endpoints.method(GARDEN_RESOURCE, GardenRecord,
                      path='garden', http_method='GET',
                      name='garden.status')
    def get_garden_api(self, request):
        garden_status = get_garden_status()
        if request.location != '':
            for garden in garden_status:
                if garden.neighbourhood.neighbourhood == request.location:
                    return GardenRecord(moistureLevel=garden.moistureLevel,
                                        temperature=garden.temperature,
                                        humidity=garden.humidity,
                                        heatIndex=garden.heatIndex)
            raise endpoints.BadRequestException("No such location record is found.")
        raise endpoints.BadRequestException("Location cannot be empty!")

    @endpoints.method(message_types.VoidMessage, NeighbourhoodCollection,
                      path='neighbourhoods', http_method='GET',
                      name='garden.neighbourhood')
    def get_neighbourhoods_api(self, unused_request):
        NEIGHBOURHOODS_LIST = []
        gardens = get_garden_status()
        added = []
        for garden in gardens:
            if garden.neighbourhood.neighbourhood not in added:
                added.append(garden.neighbourhood.neighbourhood)
                NEIGHBOURHOODS_LIST.append(SingleNeighbourhood(
                    neighbourhood=garden.neighbourhood.neighbourhood,
                    points=garden.points))
        return NeighbourhoodCollection(neighbourhoods=NEIGHBOURHOODS_LIST)

        
APPLICATION = endpoints.api_server([Api])
