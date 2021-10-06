const Ajv = require('ajv/dist/jtd')
const validators = require('./validators')
const {ajv} = require("./validators");


const hasRightGeneralSchema = (res) => {
  const body = res.body;
  if (!validators.generalSchema(body)) {
    throw new Error('The response body has the wrong general schema.');
  }
};

const hasNoErrorEntries = (res) => {
  const body = res.body;
  if (body.errors.length > 0) {
    throw new Error('Errors are being reported');
  }
};

const isOkay = (req) => {
  return req
    .expect(200)
    .expect('Content-Type', /json/)
    .expect(hasRightGeneralSchema)
    .expect(hasNoErrorEntries);
};

const checkPayloadFromSchema = (schema) => {
  return (res) => {
    const validate = ajv.compile(schema);
    if (!validate(res.body.payload)) {
      throw new Error('The payload has the wrong schema: \n' + JSON.stringify(validate.errors, null, 2));
    }
  };
}


module.exports = {
  isOkay,
  checkPayloadFromSchema
};
