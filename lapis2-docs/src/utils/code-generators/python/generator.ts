import type { ResultField, ResultFieldType } from '../types';

export function generateNonFastaQuery(host: string, endpoint: string, query: object, resultFields: ResultField[]) {
    const queryJson = JSON.stringify(query, null, 4).replaceAll('\n', '\n    ');

    return `import requests
from dataclasses import dataclass
from typing import Optional, List, Any, Union

@dataclass
class DataEntry:
${resultFields.map((f) => `    ${f.name}: ${wrapOptional(mapResultFieldType(f.type), f.nullable)}`).join('\n')}

@dataclass
class Info:
    apiVersion: int
    dataVersion: int
    deprecationDate: Optional[str]
    deprecationInfo: Optional[str]
    acknowledgement: Optional[str]

@dataclass
class LapisResponse:
    errors: List[Any]
    info: Info
    data: List[DataEntry]

def fetch_data() -> LapisResponse:
    # Define query
    lapis_host = "${host}"
    endpoint = "${endpoint}"
    url = lapis_host + endpoint
    query = ${queryJson}

    # Send request
    http_response = requests.post(url, json=query)
    
    # TODO: error handling: check status code and the errors object
    # TODO: if there is a deprecation info, print it as a warning

    # Parse response
    json_dict = http_response.json()
    lapis_response = LapisResponse(
        errors=json_dict["errors"],
        info=Info(**json_dict["info"]),
        data=[DataEntry(**d) for d in json_dict["data"]]
    )

    return lapis_response

lapis_response = fetch_data()
data = lapis_response.data

print(data)`;
}

function wrapOptional(inner: string, wrap: boolean) {
    if (wrap) {
        return `Optional[${inner}]`;
    }
    return inner;
}

function mapResultFieldType(type: ResultFieldType): 'str' | 'int' | 'float' {
    switch (type) {
        case 'string':
            return 'str';
        case 'float':
            return 'float';
        case 'integer':
            return 'int';
    }
}
