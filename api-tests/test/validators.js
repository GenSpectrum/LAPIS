const Ajv = require("ajv/dist/jtd");


const ajv = new Ajv({
  allowDate: true
});


const generalSchema = {
  properties: {
    info: {
      properties: {
        apiVersion: {type: 'uint32'},
        dataVersion: {type: 'uint32'},
        deprecationDate: {type: 'timestamp', nullable: true},
        deprecationInfo: {type: 'string', nullable: true}
      }
    },
    errors: {
      elements: {}
    },
    payload: {}
  }
};


module.exports = {
  ajv,
  generalSchema: ajv.compile(generalSchema)
};
