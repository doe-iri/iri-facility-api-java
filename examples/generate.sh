#!/bin/sh

#
# IRI Facility API reference implementation Copyright (c) 2025,
# The Regents of the University of California, through Lawrence
# Berkeley National Laboratory (subject to receipt of any required
# approvals from the U.S. Dept. of Energy).  All rights reserved.
#
# If you have questions about your rights to use or distribute this
# software, please contact Berkeley Lab's Innovation & Partnerships
# Office at IPO@lbl.gov.
#
# NOTICE.  This Software was developed under funding from the
# U.S. Department of Energy and the U.S. Government consequently retains
# certain rights. As such, the U.S. Government has been granted for
# itself and others acting on its behalf a paid-up, nonexclusive,
# irrevocable, worldwide license in the Software to reproduce,
# distribute copies to the public, prepare derivative works, and perform
# publicly and display publicly, and to permit other to do so.
#

# API Endpoints and Corresponding Output Files
ENDPOINTS=(
    "http://localhost:8081/v3/api-docs openapi_iri_facility_api_v1.json"
    "http://localhost:8081/api/v1 meta-data.json"
    "http://localhost:8081/api/v1/status status-meta-data.json"
    "http://localhost:8081/api/v1/facility facility.json"
    "http://localhost:8081/api/v1/facility/sites sites.json"
    "http://localhost:8081/api/v1/facility/locations locations.json"
    "http://localhost:8081/api/v1/status/resources resources.json"
    "http://localhost:8081/api/v1/status/incidents incidents.json"
    "http://localhost:8081/api/v1/status/events events.json"
    "http://localhost:8081/api/v1/account/projects projects.json"
    "http://localhost:8081/api/v1/account/user_allocations user_allocations.json"
    "http://localhost:8081/api/v1/account/project_allocations project_allocations.json"
    "http://localhost:8081/api/v1/account/capabilities capabilities.json"
)

# Function to fetch data
fetch_data() {
    local url=$1
    local file=$2

    echo "Fetching data from $url..."
    if curl -s "$url" | jq . > "$file"; then
        echo "\033[32m✔ Data successfully saved to $file\033[0m"
    else
        echo "\033[31m❌ Error: Failed to fetch data from $url\033[0m"
        exit 1
    fi
}

# Loop through each endpoint and fetch data
for entry in "${ENDPOINTS[@]}"; do
    fetch_data $(echo "$entry")
done
