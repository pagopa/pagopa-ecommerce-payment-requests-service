
locals {
  name                = "${var.prefix}-${var.env_short}-opex_notification-service"
  dashboard_base_addr = "https://portal.azure.com/#@pagopait.onmicrosoft.com/dashboard/arm"
}

data "azurerm_resource_group" "this" {
  name     = "dashboards"
}

resource "azurerm_portal_dashboard" "this" {
  name                = local.name
  resource_group_name = data.azurerm_resource_group.this.name
  location            = data.azurerm_resource_group.this.location

  dashboard_properties = <<-PROPS
    {
  "lenses": {
    "0": {
      "order": 0,
      "parts": {
        "0": {
          "position": {
            "x": 0,
            "y": 0,
            "colSpan": 6,
            "rowSpan": 4
          },
          "metadata": {
            "inputs": [
              {
                "name": "resourceTypeMode",
                "isOptional": true
              },
              {
                "name": "ComponentId",
                "isOptional": true
              },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
                  ]
                },
                "isOptional": true
              },
              {
                "name": "PartId",
                "isOptional": true
              },
              {
                "name": "Version",
                "value": "2.0",
                "isOptional": true
              },
              {
                "name": "TimeRange",
                "value": "PT4H",
                "isOptional": true
              },
              {
                "name": "DashboardId",
                "isOptional": true
              },
              {
                "name": "DraftRequestParameters",
                "value": {
                  "scope": "hierarchy"
                },
                "isOptional": true
              },
              {
                "name": "Query",
                "value": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 0.99;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/payment-requests/[^/]+\"\n| summarize\n  Total=count(),\n  Success=count(httpStatus_d < 500) by bin(TimeGenerated, 5m)\n| extend availability=toreal(Success) / Total\n| project TimeGenerated, availability, watermark=threshold\n| render timechart with (xtitle = \"time\", ytitle= \"availability(%)\")\n",
                "isOptional": true
              },
              {
                "name": "ControlType",
                "value": "FrameControlChart",
                "isOptional": true
              },
              {
                "name": "SpecificChart",
                "value": "Line",
                "isOptional": true
              },
              {
                "name": "PartTitle",
                "value": "Availability (5m)",
                "isOptional": true
              },
              {
                "name": "PartSubTitle",
                "value": "/payment-requests/payment-requests/{rpt_id}",
                "isOptional": true
              },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "availability",
                      "type": "real"
                    },
                    {
                      "name": "watermark",
                      "type": "real"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              {
                "name": "LegendOptions",
                "value": {
                  "isEnabled": true,
                  "position": "Bottom"
                },
                "isOptional": true
              },
              {
                "name": "IsQueryContainTimeRange",
                "value": false,
                "isOptional": true
              }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "Query": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 0.99;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/payment-requests/[^/]+\"\n| summarize\n  Total=count(),\n  Success=count(httpStatus_d < 500) by bin(TimeGenerated, 5m)\n| extend availability=toreal(Success) / Total\n| project TimeGenerated, availability, watermark=threshold\n| render timechart with (xtitle = \"time\", ytitle= \"availability(%)\")\n",
                "PartTitle": "Availability (5m)"
              }
            }
          }
        },
        "1": {
          "position": {
            "x": 6,
            "y": 0,
            "colSpan": 6,
            "rowSpan": 4
          },
          "metadata": {
            "inputs": [
              {
                "name": "resourceTypeMode",
                "isOptional": true
              },
              {
                "name": "ComponentId",
                "isOptional": true
              },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
                  ]
                },
                "isOptional": true
              },
              {
                "name": "PartId",
                "isOptional": true
              },
              {
                "name": "Version",
                "value": "2.0",
                "isOptional": true
              },
              {
                "name": "TimeRange",
                "value": "PT4H",
                "isOptional": true
              },
              {
                "name": "DashboardId",
                "isOptional": true
              },
              {
                "name": "DraftRequestParameters",
                "value": {
                  "scope": "hierarchy"
                },
                "isOptional": true
              },
              {
                "name": "Query",
                "value": "\nlet api_url = \"/payment-requests/payment-requests/[^/]+\";\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex api_url\n| extend HTTPStatus = case(\n  httpStatus_d between (100 .. 199), \"1XX\",\n  httpStatus_d between (200 .. 299), \"2XX\",\n  httpStatus_d between (300 .. 399), \"3XX\",\n  httpStatus_d between (400 .. 499), \"4XX\",\n  \"5XX\")\n| summarize count() by HTTPStatus, bin(TimeGenerated, 5m)\n| render areachart with (xtitle = \"time\", ytitle= \"count\")\n",
                "isOptional": true
              },
              {
                "name": "ControlType",
                "value": "FrameControlChart",
                "isOptional": true
              },
              {
                "name": "SpecificChart",
                "value": "Pie",
                "isOptional": true
              },
              {
                "name": "PartTitle",
                "value": "Response Codes (5m)",
                "isOptional": true
              },
              {
                "name": "PartSubTitle",
                "value": "/payment-requests/payment-requests/{rpt_id}",
                "isOptional": true
              },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": {
                    "name": "httpStatus_d",
                    "type": "string"
                  },
                  "yAxis": [
                    {
                      "name": "count_",
                      "type": "long"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              {
                "name": "LegendOptions",
                "value": {
                  "isEnabled": true,
                  "position": "Bottom"
                },
                "isOptional": true
              },
              {
                "name": "IsQueryContainTimeRange",
                "value": false,
                "isOptional": true
              }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "Query": "\nlet api_url = \"/payment-requests/payment-requests/[^/]+\";\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex api_url\n| extend HTTPStatus = case(\n  httpStatus_d between (100 .. 199), \"1XX\",\n  httpStatus_d between (200 .. 299), \"2XX\",\n  httpStatus_d between (300 .. 399), \"3XX\",\n  httpStatus_d between (400 .. 499), \"4XX\",\n  \"5XX\")\n| summarize count() by HTTPStatus, bin(TimeGenerated, 5m)\n| render areachart with (xtitle = \"time\", ytitle= \"count\")\n",
                "SpecificChart": "StackedArea",
                "PartTitle": "Response Codes (5m)",
                "Dimensions": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "count_",
                      "type": "long"
                    }
                  ],
                  "splitBy": [
                    {
                      "name": "HTTPStatus",
                      "type": "string"
                    }
                  ],
                  "aggregation": "Sum"
                }
              }
            }
          }
        },
        "2": {
          "position": {
            "x": 12,
            "y": 0,
            "colSpan": 6,
            "rowSpan": 4
          },
          "metadata": {
            "inputs": [
              {
                "name": "resourceTypeMode",
                "isOptional": true
              },
              {
                "name": "ComponentId",
                "isOptional": true
              },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
                  ]
                },
                "isOptional": true
              },
              {
                "name": "PartId",
                "isOptional": true
              },
              {
                "name": "Version",
                "value": "2.0",
                "isOptional": true
              },
              {
                "name": "TimeRange",
                "value": "PT4H",
                "isOptional": true
              },
              {
                "name": "DashboardId",
                "isOptional": true
              },
              {
                "name": "DraftRequestParameters",
                "value": {
                  "scope": "hierarchy"
                },
                "isOptional": true
              },
              {
                "name": "Query",
                "value": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 1;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/payment-requests/[^/]+\"\n| summarize\n    watermark=threshold,\n    duration_percentile_95=percentiles(timeTaken_d, 95) by bin(TimeGenerated, 5m)\n| render timechart with (xtitle = \"time\", ytitle= \"response time(s)\")\n",
                "isOptional": true
              },
              {
                "name": "ControlType",
                "value": "FrameControlChart",
                "isOptional": true
              },
              {
                "name": "SpecificChart",
                "value": "StackedColumn",
                "isOptional": true
              },
              {
                "name": "PartTitle",
                "value": "Percentile Response Time (5m)",
                "isOptional": true
              },
              {
                "name": "PartSubTitle",
                "value": "/payment-requests/payment-requests/{rpt_id}",
                "isOptional": true
              },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "duration_percentile_95",
                      "type": "real"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              {
                "name": "LegendOptions",
                "value": {
                  "isEnabled": true,
                  "position": "Bottom"
                },
                "isOptional": true
              },
              {
                "name": "IsQueryContainTimeRange",
                "value": false,
                "isOptional": true
              }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "Query": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 1;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/payment-requests/[^/]+\"\n| summarize\n    watermark=threshold,\n    duration_percentile_95=percentiles(timeTaken_d, 95) by bin(TimeGenerated, 5m)\n| render timechart with (xtitle = \"time\", ytitle= \"response time(s)\")\n",
                "SpecificChart": "Line",
                "PartTitle": "Percentile Response Time (5m)",
                "Dimensions": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "watermark",
                      "type": "long"
                    },
                    {
                      "name": "duration_percentile_95",
                      "type": "real"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                }
              }
            }
          }
        },
        "3": {
          "position": {
            "x": 0,
            "y": 4,
            "colSpan": 6,
            "rowSpan": 4
          },
          "metadata": {
            "inputs": [
              {
                "name": "resourceTypeMode",
                "isOptional": true
              },
              {
                "name": "ComponentId",
                "isOptional": true
              },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
                  ]
                },
                "isOptional": true
              },
              {
                "name": "PartId",
                "isOptional": true
              },
              {
                "name": "Version",
                "value": "2.0",
                "isOptional": true
              },
              {
                "name": "TimeRange",
                "value": "PT4H",
                "isOptional": true
              },
              {
                "name": "DashboardId",
                "isOptional": true
              },
              {
                "name": "DraftRequestParameters",
                "value": {
                  "scope": "hierarchy"
                },
                "isOptional": true
              },
              {
                "name": "Query",
                "value": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 0.99;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/carts\"\n| summarize\n  Total=count(),\n  Success=count(httpStatus_d < 500) by bin(TimeGenerated, 5m)\n| extend availability=toreal(Success) / Total\n| project TimeGenerated, availability, watermark=threshold\n| render timechart with (xtitle = \"time\", ytitle= \"availability(%)\")\n",
                "isOptional": true
              },
              {
                "name": "ControlType",
                "value": "FrameControlChart",
                "isOptional": true
              },
              {
                "name": "SpecificChart",
                "value": "Line",
                "isOptional": true
              },
              {
                "name": "PartTitle",
                "value": "Availability (5m)",
                "isOptional": true
              },
              {
                "name": "PartSubTitle",
                "value": "/payment-requests/carts",
                "isOptional": true
              },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "availability",
                      "type": "real"
                    },
                    {
                      "name": "watermark",
                      "type": "real"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              {
                "name": "LegendOptions",
                "value": {
                  "isEnabled": true,
                  "position": "Bottom"
                },
                "isOptional": true
              },
              {
                "name": "IsQueryContainTimeRange",
                "value": false,
                "isOptional": true
              }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "Query": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 0.99;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/carts\"\n| summarize\n  Total=count(),\n  Success=count(httpStatus_d < 500) by bin(TimeGenerated, 5m)\n| extend availability=toreal(Success) / Total\n| project TimeGenerated, availability, watermark=threshold\n| render timechart with (xtitle = \"time\", ytitle= \"availability(%)\")\n",
                "PartTitle": "Availability (5m)"
              }
            }
          }
        },
        "4": {
          "position": {
            "x": 6,
            "y": 4,
            "colSpan": 6,
            "rowSpan": 4
          },
          "metadata": {
            "inputs": [
              {
                "name": "resourceTypeMode",
                "isOptional": true
              },
              {
                "name": "ComponentId",
                "isOptional": true
              },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
                  ]
                },
                "isOptional": true
              },
              {
                "name": "PartId",
                "isOptional": true
              },
              {
                "name": "Version",
                "value": "2.0",
                "isOptional": true
              },
              {
                "name": "TimeRange",
                "value": "PT4H",
                "isOptional": true
              },
              {
                "name": "DashboardId",
                "isOptional": true
              },
              {
                "name": "DraftRequestParameters",
                "value": {
                  "scope": "hierarchy"
                },
                "isOptional": true
              },
              {
                "name": "Query",
                "value": "\nlet api_url = \"/payment-requests/carts\";\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex api_url\n| extend HTTPStatus = case(\n  httpStatus_d between (100 .. 199), \"1XX\",\n  httpStatus_d between (200 .. 299), \"2XX\",\n  httpStatus_d between (300 .. 399), \"3XX\",\n  httpStatus_d between (400 .. 499), \"4XX\",\n  \"5XX\")\n| summarize count() by HTTPStatus, bin(TimeGenerated, 5m)\n| render areachart with (xtitle = \"time\", ytitle= \"count\")\n",
                "isOptional": true
              },
              {
                "name": "ControlType",
                "value": "FrameControlChart",
                "isOptional": true
              },
              {
                "name": "SpecificChart",
                "value": "Pie",
                "isOptional": true
              },
              {
                "name": "PartTitle",
                "value": "Response Codes (5m)",
                "isOptional": true
              },
              {
                "name": "PartSubTitle",
                "value": "/payment-requests/carts",
                "isOptional": true
              },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": {
                    "name": "httpStatus_d",
                    "type": "string"
                  },
                  "yAxis": [
                    {
                      "name": "count_",
                      "type": "long"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              {
                "name": "LegendOptions",
                "value": {
                  "isEnabled": true,
                  "position": "Bottom"
                },
                "isOptional": true
              },
              {
                "name": "IsQueryContainTimeRange",
                "value": false,
                "isOptional": true
              }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "Query": "\nlet api_url = \"/payment-requests/carts\";\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex api_url\n| extend HTTPStatus = case(\n  httpStatus_d between (100 .. 199), \"1XX\",\n  httpStatus_d between (200 .. 299), \"2XX\",\n  httpStatus_d between (300 .. 399), \"3XX\",\n  httpStatus_d between (400 .. 499), \"4XX\",\n  \"5XX\")\n| summarize count() by HTTPStatus, bin(TimeGenerated, 5m)\n| render areachart with (xtitle = \"time\", ytitle= \"count\")\n",
                "SpecificChart": "StackedArea",
                "PartTitle": "Response Codes (5m)",
                "Dimensions": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "count_",
                      "type": "long"
                    }
                  ],
                  "splitBy": [
                    {
                      "name": "HTTPStatus",
                      "type": "string"
                    }
                  ],
                  "aggregation": "Sum"
                }
              }
            }
          }
        },
        "5": {
          "position": {
            "x": 12,
            "y": 4,
            "colSpan": 6,
            "rowSpan": 4
          },
          "metadata": {
            "inputs": [
              {
                "name": "resourceTypeMode",
                "isOptional": true
              },
              {
                "name": "ComponentId",
                "isOptional": true
              },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
                  ]
                },
                "isOptional": true
              },
              {
                "name": "PartId",
                "isOptional": true
              },
              {
                "name": "Version",
                "value": "2.0",
                "isOptional": true
              },
              {
                "name": "TimeRange",
                "value": "PT4H",
                "isOptional": true
              },
              {
                "name": "DashboardId",
                "isOptional": true
              },
              {
                "name": "DraftRequestParameters",
                "value": {
                  "scope": "hierarchy"
                },
                "isOptional": true
              },
              {
                "name": "Query",
                "value": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 1;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/carts\"\n| summarize\n    watermark=threshold,\n    duration_percentile_95=percentiles(timeTaken_d, 95) by bin(TimeGenerated, 5m)\n| render timechart with (xtitle = \"time\", ytitle= \"response time(s)\")\n",
                "isOptional": true
              },
              {
                "name": "ControlType",
                "value": "FrameControlChart",
                "isOptional": true
              },
              {
                "name": "SpecificChart",
                "value": "StackedColumn",
                "isOptional": true
              },
              {
                "name": "PartTitle",
                "value": "Percentile Response Time (5m)",
                "isOptional": true
              },
              {
                "name": "PartSubTitle",
                "value": "/payment-requests/carts",
                "isOptional": true
              },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "duration_percentile_95",
                      "type": "real"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              {
                "name": "LegendOptions",
                "value": {
                  "isEnabled": true,
                  "position": "Bottom"
                },
                "isOptional": true
              },
              {
                "name": "IsQueryContainTimeRange",
                "value": false,
                "isOptional": true
              }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "Query": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 1;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/carts\"\n| summarize\n    watermark=threshold,\n    duration_percentile_95=percentiles(timeTaken_d, 95) by bin(TimeGenerated, 5m)\n| render timechart with (xtitle = \"time\", ytitle= \"response time(s)\")\n",
                "SpecificChart": "Line",
                "PartTitle": "Percentile Response Time (5m)",
                "Dimensions": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "watermark",
                      "type": "long"
                    },
                    {
                      "name": "duration_percentile_95",
                      "type": "real"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                }
              }
            }
          }
        },
        "6": {
          "position": {
            "x": 0,
            "y": 8,
            "colSpan": 6,
            "rowSpan": 4
          },
          "metadata": {
            "inputs": [
              {
                "name": "resourceTypeMode",
                "isOptional": true
              },
              {
                "name": "ComponentId",
                "isOptional": true
              },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
                  ]
                },
                "isOptional": true
              },
              {
                "name": "PartId",
                "isOptional": true
              },
              {
                "name": "Version",
                "value": "2.0",
                "isOptional": true
              },
              {
                "name": "TimeRange",
                "value": "PT4H",
                "isOptional": true
              },
              {
                "name": "DashboardId",
                "isOptional": true
              },
              {
                "name": "DraftRequestParameters",
                "value": {
                  "scope": "hierarchy"
                },
                "isOptional": true
              },
              {
                "name": "Query",
                "value": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 0.99;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/carts/[^/]+\"\n| summarize\n  Total=count(),\n  Success=count(httpStatus_d < 500) by bin(TimeGenerated, 5m)\n| extend availability=toreal(Success) / Total\n| project TimeGenerated, availability, watermark=threshold\n| render timechart with (xtitle = \"time\", ytitle= \"availability(%)\")\n",
                "isOptional": true
              },
              {
                "name": "ControlType",
                "value": "FrameControlChart",
                "isOptional": true
              },
              {
                "name": "SpecificChart",
                "value": "Line",
                "isOptional": true
              },
              {
                "name": "PartTitle",
                "value": "Availability (5m)",
                "isOptional": true
              },
              {
                "name": "PartSubTitle",
                "value": "/payment-requests/carts/{id_cart}",
                "isOptional": true
              },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "availability",
                      "type": "real"
                    },
                    {
                      "name": "watermark",
                      "type": "real"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              {
                "name": "LegendOptions",
                "value": {
                  "isEnabled": true,
                  "position": "Bottom"
                },
                "isOptional": true
              },
              {
                "name": "IsQueryContainTimeRange",
                "value": false,
                "isOptional": true
              }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "Query": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 0.99;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/carts/[^/]+\"\n| summarize\n  Total=count(),\n  Success=count(httpStatus_d < 500) by bin(TimeGenerated, 5m)\n| extend availability=toreal(Success) / Total\n| project TimeGenerated, availability, watermark=threshold\n| render timechart with (xtitle = \"time\", ytitle= \"availability(%)\")\n",
                "PartTitle": "Availability (5m)"
              }
            }
          }
        },
        "7": {
          "position": {
            "x": 6,
            "y": 8,
            "colSpan": 6,
            "rowSpan": 4
          },
          "metadata": {
            "inputs": [
              {
                "name": "resourceTypeMode",
                "isOptional": true
              },
              {
                "name": "ComponentId",
                "isOptional": true
              },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
                  ]
                },
                "isOptional": true
              },
              {
                "name": "PartId",
                "isOptional": true
              },
              {
                "name": "Version",
                "value": "2.0",
                "isOptional": true
              },
              {
                "name": "TimeRange",
                "value": "PT4H",
                "isOptional": true
              },
              {
                "name": "DashboardId",
                "isOptional": true
              },
              {
                "name": "DraftRequestParameters",
                "value": {
                  "scope": "hierarchy"
                },
                "isOptional": true
              },
              {
                "name": "Query",
                "value": "\nlet api_url = \"/payment-requests/carts/[^/]+\";\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex api_url\n| extend HTTPStatus = case(\n  httpStatus_d between (100 .. 199), \"1XX\",\n  httpStatus_d between (200 .. 299), \"2XX\",\n  httpStatus_d between (300 .. 399), \"3XX\",\n  httpStatus_d between (400 .. 499), \"4XX\",\n  \"5XX\")\n| summarize count() by HTTPStatus, bin(TimeGenerated, 5m)\n| render areachart with (xtitle = \"time\", ytitle= \"count\")\n",
                "isOptional": true
              },
              {
                "name": "ControlType",
                "value": "FrameControlChart",
                "isOptional": true
              },
              {
                "name": "SpecificChart",
                "value": "Pie",
                "isOptional": true
              },
              {
                "name": "PartTitle",
                "value": "Response Codes (5m)",
                "isOptional": true
              },
              {
                "name": "PartSubTitle",
                "value": "/payment-requests/carts/{id_cart}",
                "isOptional": true
              },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": {
                    "name": "httpStatus_d",
                    "type": "string"
                  },
                  "yAxis": [
                    {
                      "name": "count_",
                      "type": "long"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              {
                "name": "LegendOptions",
                "value": {
                  "isEnabled": true,
                  "position": "Bottom"
                },
                "isOptional": true
              },
              {
                "name": "IsQueryContainTimeRange",
                "value": false,
                "isOptional": true
              }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "Query": "\nlet api_url = \"/payment-requests/carts/[^/]+\";\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex api_url\n| extend HTTPStatus = case(\n  httpStatus_d between (100 .. 199), \"1XX\",\n  httpStatus_d between (200 .. 299), \"2XX\",\n  httpStatus_d between (300 .. 399), \"3XX\",\n  httpStatus_d between (400 .. 499), \"4XX\",\n  \"5XX\")\n| summarize count() by HTTPStatus, bin(TimeGenerated, 5m)\n| render areachart with (xtitle = \"time\", ytitle= \"count\")\n",
                "SpecificChart": "StackedArea",
                "PartTitle": "Response Codes (5m)",
                "Dimensions": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "count_",
                      "type": "long"
                    }
                  ],
                  "splitBy": [
                    {
                      "name": "HTTPStatus",
                      "type": "string"
                    }
                  ],
                  "aggregation": "Sum"
                }
              }
            }
          }
        },
        "8": {
          "position": {
            "x": 12,
            "y": 8,
            "colSpan": 6,
            "rowSpan": 4
          },
          "metadata": {
            "inputs": [
              {
                "name": "resourceTypeMode",
                "isOptional": true
              },
              {
                "name": "ComponentId",
                "isOptional": true
              },
              {
                "name": "Scope",
                "value": {
                  "resourceIds": [
                    "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
                  ]
                },
                "isOptional": true
              },
              {
                "name": "PartId",
                "isOptional": true
              },
              {
                "name": "Version",
                "value": "2.0",
                "isOptional": true
              },
              {
                "name": "TimeRange",
                "value": "PT4H",
                "isOptional": true
              },
              {
                "name": "DashboardId",
                "isOptional": true
              },
              {
                "name": "DraftRequestParameters",
                "value": {
                  "scope": "hierarchy"
                },
                "isOptional": true
              },
              {
                "name": "Query",
                "value": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 1;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/carts/[^/]+\"\n| summarize\n    watermark=threshold,\n    duration_percentile_95=percentiles(timeTaken_d, 95) by bin(TimeGenerated, 5m)\n| render timechart with (xtitle = \"time\", ytitle= \"response time(s)\")\n",
                "isOptional": true
              },
              {
                "name": "ControlType",
                "value": "FrameControlChart",
                "isOptional": true
              },
              {
                "name": "SpecificChart",
                "value": "StackedColumn",
                "isOptional": true
              },
              {
                "name": "PartTitle",
                "value": "Percentile Response Time (5m)",
                "isOptional": true
              },
              {
                "name": "PartSubTitle",
                "value": "/payment-requests/carts/{id_cart}",
                "isOptional": true
              },
              {
                "name": "Dimensions",
                "value": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "duration_percentile_95",
                      "type": "real"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                },
                "isOptional": true
              },
              {
                "name": "LegendOptions",
                "value": {
                  "isEnabled": true,
                  "position": "Bottom"
                },
                "isOptional": true
              },
              {
                "name": "IsQueryContainTimeRange",
                "value": false,
                "isOptional": true
              }
            ],
            "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
            "settings": {
              "content": {
                "Query": "\nlet api_hosts = datatable (name: string) [\"api.platform.pagopa.it\"];\nlet threshold = 1;\nAzureDiagnostics\n| where originalHost_s in (api_hosts)\n| where requestUri_s matches regex \"/payment-requests/carts/[^/]+\"\n| summarize\n    watermark=threshold,\n    duration_percentile_95=percentiles(timeTaken_d, 95) by bin(TimeGenerated, 5m)\n| render timechart with (xtitle = \"time\", ytitle= \"response time(s)\")\n",
                "SpecificChart": "Line",
                "PartTitle": "Percentile Response Time (5m)",
                "Dimensions": {
                  "xAxis": {
                    "name": "TimeGenerated",
                    "type": "datetime"
                  },
                  "yAxis": [
                    {
                      "name": "watermark",
                      "type": "long"
                    },
                    {
                      "name": "duration_percentile_95",
                      "type": "real"
                    }
                  ],
                  "splitBy": [],
                  "aggregation": "Sum"
                }
              }
            }
          }
        }
      }
    }
  },
  "metadata": {
    "model": {
      "timeRange": {
        "value": {
          "relative": {
            "duration": 24,
            "timeUnit": 1
          }
        },
        "type": "MsPortalFx.Composition.Configuration.ValueTypes.TimeRange"
      },
      "filterLocale": {
        "value": "en-us"
      },
      "filters": {
        "value": {
          "MsPortalFx_TimeRange": {
            "model": {
              "format": "local",
              "granularity": "auto",
              "relative": "48h"
            },
            "displayCache": {
              "name": "Local Time",
              "value": "Past 48 hours"
            },
            "filteredPartIds": [
              "StartboardPart-LogsDashboardPart-9badbd78-7607-4131-8fa1-8b85191432ed",
              "StartboardPart-LogsDashboardPart-9badbd78-7607-4131-8fa1-8b85191432ef",
              "StartboardPart-LogsDashboardPart-9badbd78-7607-4131-8fa1-8b85191432f1",
              "StartboardPart-LogsDashboardPart-9badbd78-7607-4131-8fa1-8b85191432f3",
              "StartboardPart-LogsDashboardPart-9badbd78-7607-4131-8fa1-8b85191432f5",
              "StartboardPart-LogsDashboardPart-9badbd78-7607-4131-8fa1-8b85191432f7",
              "StartboardPart-LogsDashboardPart-9badbd78-7607-4131-8fa1-8b85191432f9",
              "StartboardPart-LogsDashboardPart-9badbd78-7607-4131-8fa1-8b85191432fb",
              "StartboardPart-LogsDashboardPart-9badbd78-7607-4131-8fa1-8b85191432fd"
            ]
          }
        }
      }
    }
  }
}
  PROPS

  tags = var.tags
}


resource "azurerm_monitor_scheduled_query_rules_alert" "alarm_availability_0" {
  name                = replace(join("_",split("/", "${local.name}-availability @ /payment-requests/payment-requests/{rpt_id}")), "/\\{|\\}/", "")
  resource_group_name = data.azurerm_resource_group.this.name
  location            = data.azurerm_resource_group.this.location

  action {
    action_group = ["/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/PagoPA", "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/SlackPagoPA"]
  }

  data_source_id          = "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
  description             = "Availability for /payment-requests/payment-requests/{rpt_id} is less than or equal to 99% - ${local.dashboard_base_addr}${azurerm_portal_dashboard.this.id}"
  enabled                 = true
  auto_mitigation_enabled = false

  query = <<-QUERY

    
let api_hosts = datatable (name: string) ["api.platform.pagopa.it"];
let threshold = 0.99;
AzureDiagnostics
| where originalHost_s in (api_hosts)
| where requestUri_s matches regex "/payment-requests/payment-requests/[^/]+"
| summarize
  Total=count(),
  Success=count(httpStatus_d < 500) by bin(TimeGenerated, 5m)
| extend availability=toreal(Success) / Total
| where availability < threshold


  QUERY

  severity    = 1
  frequency   = 10
  time_window = 20
  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }

  tags = var.tags
}

resource "azurerm_monitor_scheduled_query_rules_alert" "alarm_time_0" {
  name                = replace(join("_",split("/", "${local.name}-responsetime @ /payment-requests/payment-requests/{rpt_id}")), "/\\{|\\}/", "")
  resource_group_name = data.azurerm_resource_group.this.name
  location            = data.azurerm_resource_group.this.location

  action {
    action_group = ["/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/PagoPA", "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/SlackPagoPA"]
  }

  data_source_id          = "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
  description             = "Response time for /payment-requests/payment-requests/{rpt_id} is less than or equal to 1s - ${local.dashboard_base_addr}${azurerm_portal_dashboard.this.id}"
  enabled                 = true
  auto_mitigation_enabled = false

  query = <<-QUERY

    
let api_hosts = datatable (name: string) ["api.platform.pagopa.it"];
let threshold = 1;
AzureDiagnostics
| where originalHost_s in (api_hosts)
| where requestUri_s matches regex "/payment-requests/payment-requests/[^/]+"
| summarize
    watermark=threshold,
    duration_percentile_95=percentiles(timeTaken_d, 95) by bin(TimeGenerated, 5m)
| where duration_percentile_95 > threshold


  QUERY

  severity    = 1
  frequency   = 10
  time_window = 20
  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }

  tags = var.tags
}

resource "azurerm_monitor_scheduled_query_rules_alert" "alarm_availability_1" {
  name                = replace(join("_",split("/", "${local.name}-availability @ /payment-requests/carts")), "/\\{|\\}/", "")
  resource_group_name = data.azurerm_resource_group.this.name
  location            = data.azurerm_resource_group.this.location

  action {
    action_group = ["/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/PagoPA", "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/SlackPagoPA"]
  }

  data_source_id          = "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
  description             = "Availability for /payment-requests/carts is less than or equal to 99% - ${local.dashboard_base_addr}${azurerm_portal_dashboard.this.id}"
  enabled                 = true
  auto_mitigation_enabled = false

  query = <<-QUERY

    
let api_hosts = datatable (name: string) ["api.platform.pagopa.it"];
let threshold = 0.99;
AzureDiagnostics
| where originalHost_s in (api_hosts)
| where requestUri_s matches regex "/payment-requests/carts"
| summarize
  Total=count(),
  Success=count(httpStatus_d < 500) by bin(TimeGenerated, 5m)
| extend availability=toreal(Success) / Total
| where availability < threshold


  QUERY

  severity    = 1
  frequency   = 10
  time_window = 20
  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }

  tags = var.tags
}

resource "azurerm_monitor_scheduled_query_rules_alert" "alarm_time_1" {
  name                = replace(join("_",split("/", "${local.name}-responsetime @ /payment-requests/carts")), "/\\{|\\}/", "")
  resource_group_name = data.azurerm_resource_group.this.name
  location            = data.azurerm_resource_group.this.location

  action {
    action_group = ["/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/PagoPA", "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/SlackPagoPA"]
  }

  data_source_id          = "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
  description             = "Response time for /payment-requests/carts is less than or equal to 1s - ${local.dashboard_base_addr}${azurerm_portal_dashboard.this.id}"
  enabled                 = true
  auto_mitigation_enabled = false

  query = <<-QUERY

    
let api_hosts = datatable (name: string) ["api.platform.pagopa.it"];
let threshold = 1;
AzureDiagnostics
| where originalHost_s in (api_hosts)
| where requestUri_s matches regex "/payment-requests/carts"
| summarize
    watermark=threshold,
    duration_percentile_95=percentiles(timeTaken_d, 95) by bin(TimeGenerated, 5m)
| where duration_percentile_95 > threshold


  QUERY

  severity    = 1
  frequency   = 10
  time_window = 20
  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }

  tags = var.tags
}

resource "azurerm_monitor_scheduled_query_rules_alert" "alarm_availability_2" {
  name                = replace(join("_",split("/", "${local.name}-availability @ /payment-requests/carts/{id_cart}")), "/\\{|\\}/", "")
  resource_group_name = data.azurerm_resource_group.this.name
  location            = data.azurerm_resource_group.this.location

  action {
    action_group = ["/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/PagoPA", "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/SlackPagoPA"]
  }

  data_source_id          = "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
  description             = "Availability for /payment-requests/carts/{id_cart} is less than or equal to 99% - ${local.dashboard_base_addr}${azurerm_portal_dashboard.this.id}"
  enabled                 = true
  auto_mitigation_enabled = false

  query = <<-QUERY

    
let api_hosts = datatable (name: string) ["api.platform.pagopa.it"];
let threshold = 0.99;
AzureDiagnostics
| where originalHost_s in (api_hosts)
| where requestUri_s matches regex "/payment-requests/carts/[^/]+"
| summarize
  Total=count(),
  Success=count(httpStatus_d < 500) by bin(TimeGenerated, 5m)
| extend availability=toreal(Success) / Total
| where availability < threshold


  QUERY

  severity    = 1
  frequency   = 10
  time_window = 20
  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }

  tags = var.tags
}

resource "azurerm_monitor_scheduled_query_rules_alert" "alarm_time_2" {
  name                = replace(join("_",split("/", "${local.name}-responsetime @ /payment-requests/carts/{id_cart}")), "/\\{|\\}/", "")
  resource_group_name = data.azurerm_resource_group.this.name
  location            = data.azurerm_resource_group.this.location

  action {
    action_group = ["/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/PagoPA", "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-monitor-rg/providers/microsoft.insights/actionGroups/SlackPagoPA"]
  }

  data_source_id          = "/subscriptions/b9fc9419-6097-45fe-9f74-ba0641c91912/resourceGroups/pagopa-p-vnet-rg/providers/Microsoft.Network/applicationGateways/pagopa-p-app-gw"
  description             = "Response time for /payment-requests/carts/{id_cart} is less than or equal to 1s - ${local.dashboard_base_addr}${azurerm_portal_dashboard.this.id}"
  enabled                 = true
  auto_mitigation_enabled = false

  query = <<-QUERY

    
let api_hosts = datatable (name: string) ["api.platform.pagopa.it"];
let threshold = 1;
AzureDiagnostics
| where originalHost_s in (api_hosts)
| where requestUri_s matches regex "/payment-requests/carts/[^/]+"
| summarize
    watermark=threshold,
    duration_percentile_95=percentiles(timeTaken_d, 95) by bin(TimeGenerated, 5m)
| where duration_percentile_95 > threshold


  QUERY

  severity    = 1
  frequency   = 10
  time_window = 20
  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }

  tags = var.tags
}

