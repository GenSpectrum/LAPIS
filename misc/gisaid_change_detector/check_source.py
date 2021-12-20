#!/usr/bin/env python3

import requests
import json
import os


source_url = "https://www.epicov.org/<missing>"
lapis_info_url = "https://lapis.cov-spectrum.org/gisaid/v1/sample/info"
slack_hook = "https://hooks.slack.com/services/<missing>"
exec_cmd = "echo missing"


def read_state():
    with open("state.json") as f:
        return json.load(f)


def write_state(state):
    with open("state.json", "w") as f:
        json.dump(state, f)


def send_message(text):
    print(text)
    requests.post(slack_hook, data=json.dumps({"text": text}))


def main():
    # Current state
    state = read_state()
    print("Current state:")
    print(state)

    # Check data source
    res = requests.head(source_url)
    headers = res.headers
    content_length = headers["Content-Length"]
    last_modified = headers["Last-Modified"]
    print("Current: Content-Length={}, Last-Modified={}".format(content_length, last_modified))
    if content_length == state["content_length"] and last_modified == state["last_modified"]:
        print("No new data - bye!")
        return

    # If there is new data, the current LAPIS data version will be compared with the old "lapis_data_version"
    # field in the state. If they are the same, it means that the previous job has not yet finished (or failed).
    # In that case we will not trigger a new job.
    lapis_data_version = requests.get(lapis_info_url).json()["dataVersion"]
    if lapis_data_version == state["lapis_data_version"]:
        send_message("New data found, but the previous job has not yet finished (or failed). Nothing to do - bye!")
        return

    # Trigger a new job
    print("New data found, triggering a new job...")
    os.system(exec_cmd)
    send_message("New data found, a LAPIS import job was triggered.")

    # Update state
    new_state = {
        "content_length": content_length,
        "last_modified": last_modified,
        "lapis_data_version": lapis_data_version
    }
    write_state(new_state)
    print("Bye!")


if __name__ == "__main__":
    main()
