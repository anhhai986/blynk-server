import {fromJS, Map} from 'immutable';

const initialState = Map({
  devices: []
});

export default function Devices(state = initialState, action) {

  switch (action.type) {
    case "API_DEVICES_FETCH_SUCCESS":

      //@todo remove when backend be fixed

      const devices = [{
        "id": 0,
        "product": {
          name: "My Product"
        },
        "orgName": "Blynk Inc.",
        "name": "My New Device",
        "token": "18aaf2a2a2ae4c9e9ba0cf9c0e1ef339",
        "status": "OFFLINE",
        "disconnectTime": 0,
        "metaFields": [{
          "type": "Number",
          "name": "Jopa",
          "role": "STAFF",
          "value": 123.0
        }]
      }];

      return state.set('devices',
        fromJS(devices).map((device) => device.set('metaFields', fromJS([
          {
            type: "Coordinates",
            name: "Farm Location",
            role: "ADMIN",
            lat: 22.222,
            lon: 23.333
          },
          {
            type: "Cost",
            name: "Water Cost",
            role: "ADMIN",
            currency: "USD",
            price: 9.00,
            perValue: 2.5,
            units: "Gallon"
          },
          {
            type: "Time",
            name: "Some Time",
            role: "ADMIN",
            time: 1492697668006
          },
          {
            type: "Contact",
            name: "Smith Contacts",
            role: "ADMIN",
            contact: "Tech Support",
            firstName: "Dmitriy",
            isFirstNameEnabled: false,
            lastName: "Dumanskiy",
            isLastNameEnabled: false,
            email: "dmitriy@blynk.cc",
            isEmailEnabled: false,
            phone: "+38063673333",
            isPhoneEnabled: false,
            streetAddress: "My street",
            isStreetAddressEnabled: false,
            city: "Kyiv",
            isCityEnabled: false,
            state: "Ukraine",
            isStateEnabled: false,
            zip: "03322",
            isZipEnabled: false,
            isDefaultsEnabled: false
          },
          {
            type: "Range",
            name: "Range for Farm of Smith",
            role: "ADMIN",
            from: 60,
            to: 120
          },
          {
            type: "Measurement",
            name: "Farm of Smith",
            role: "ADMIN",
            units: "Celsius",
            value: 36
          },
            {
              type: "Text",
              name: "Device Name",
              role: "ADMIN",
              value: "My Device 0"
            },
            {
              type: "Text",
              name: "Device Owner",
              role: "ADMIN",
              value: "ihor.bra@gmail.com"
            },
            {
              type: "Text",
              name: "Location Name",
              role: "ADMIN",
              value: "Trenton New York Farm"
            },
          {
            type: "Number",
            name: "Cost of Pump 1",
            role: "ADMIN",
            value: 10.23
            }
          ]))
        )
      );

    case "API_DEVICES_UPDATE_SUCCESS":
      return state.set('devices',
        fromJS(action.payload.data).map((device) => device.set('metaFields', fromJS([
            {
              type: "Text",
              name: "Device Name",
              role: "ADMIN",
              value: "My Device 0"
            },
            {
              type: "Text",
              name: "Device Owner",
              role: "ADMIN",
              value: "ihor.bra@gmail.com"
            },
            {
              type: "Text",
              name: "Location Name",
              role: "ADMIN",
              value: "Trenton New York Farm"
            }
          ]))
        )
      );

    default:
      return state;
  }

}
