/*eslint-disable jsx-singlequotes*/
/*eslint-disable jsx-quotes*/

import React from 'react';
import { Table, Collapse, Switch } from 'antd';
import './styles.less';
import PropTypes from 'prop-types';
import LinearIcon from "../../../../../../components/LinearIcon";

const Panel = Collapse.Panel;

const PERMISSIONS_TABLE = [
  {
    dependentPermsToActivate: [6],
    dependentPermsToRemove: [],
    key: 'ORG_SWITCH',
    value: 'Switch to Sub-Organizations'
  },
  {
    dependentPermsToActivate: [6],
    dependentPermsToRemove: [],
    key: 'OWN_ORG_EDIT',
    value: 'Access Organization Settings'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [],
    key: 'OTA_VIEW',
    value: 'Get Access'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [],
    key: 'OTA_START',
    value: 'Initiate FOTA'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [],
    key: 'OTA_STOP',
    value: 'Stop/Pause FOTA'
  },
  {
    dependentPermsToActivate: [6],
    dependentPermsToRemove: [],
    key: 'ORG_CREATE',
    value: 'Create'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [5, 7, 8, 0, 1],
    key: 'ORG_VIEW',
    value: 'View'
  },
  {
    dependentPermsToActivate: [6],
    dependentPermsToRemove: [],
    key: 'ORG_EDIT',
    value: 'Edit'
  },
  {
    dependentPermsToActivate: [6],
    dependentPermsToRemove: [],
    key: 'ORG_DELETE',
    value: 'Delete'
  },
  {
    dependentPermsToActivate: [10],
    dependentPermsToRemove: [],
    key: 'ORG_INVITE_USERS',
    value: 'Invite new users'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [9, 11, 12],
    key: 'ORG_VIEW_USERS',
    value: 'View'
  },
  {
    dependentPermsToActivate: [10],
    dependentPermsToRemove: [],
    key: 'ORG_EDIT_USERS',
    value: 'Edit'
  },
  {
    dependentPermsToActivate: [10],
    dependentPermsToRemove: [],
    key: 'ORG_DELETE_USERS',
    value: 'Delete'
  },
  {
    dependentPermsToActivate: [14],
    dependentPermsToRemove: [],
    key: 'PRODUCT_CREATE',
    value: 'Add new'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [13, 15, 16],
    key: 'PRODUCT_VIEW',
    value: 'View'
  },
  {
    dependentPermsToActivate: [14],
    dependentPermsToRemove: [],
    key: 'PRODUCT_EDIT',
    value: 'Edit'
  },
  {
    dependentPermsToActivate: [14],
    dependentPermsToRemove: [],
    key: 'PRODUCT_DELETE',
    value: 'Delete'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [],
    key: 'ROLE_CREATE',
    value: 'Create new roles'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [],
    key: 'ROLE_VIEW',
    value: 'View roles and permissions'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [],
    key: 'ROLE_EDIT',
    value: 'Edit roles'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [],
    key: 'ROLE_DELETE',
    value: 'Delete roles'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [],
    key: 'ORG_DEVICES_CREATE',
    value: 'Add new devices'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [23, 24, 31],
    key: 'ORG_DEVICES_VIEW',
    value: 'View'
  },
  {
    dependentPermsToActivate: [22],
    dependentPermsToRemove: [31],
    key: 'ORG_DEVICES_EDIT',
    value: 'Edit'
  },
  {
    dependentPermsToActivate: [22],
    dependentPermsToRemove: [],
    key: 'ORG_DEVICES_DELETE',
    value: 'Delete'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [],
    key: 'ORG_DEVICES_SHARE',
    value: 'Share access'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [],
    key: 'OWN_DEVICES_CREATE',
    value: 'Add new devices'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [28, 29, 31],
    key: 'OWN_DEVICES_VIEW',
    value: 'View'
  },
  {
    dependentPermsToActivate: [27],
    dependentPermsToRemove: [31],
    key: 'OWN_DEVICES_EDIT',
    value: 'Edit'
  },
  {
    dependentPermsToActivate: [27],
    dependentPermsToRemove: [],
    key: 'OWN_DEVICES_DELETE',
    value: 'Delete'
  },
  {
    dependentPermsToActivate: [],
    dependentPermsToRemove: [],
    key: 'OWN_DEVICES_SHARE',
    value: 'Share access'
  },
  {
    dependentPermsToActivate: [27,28],
    dependentPermsToRemove: [],
    key: 'SET_AUTH_TOKEN',
    value: 'Enable Auth Token Edit'
  },
];

class RolesAndPermissions extends React.Component {

  static propTypes = {
    roles: PropTypes.array,
    GetPermissions: PropTypes.func,
    UpdateRole: PropTypes.func,
  };

  constructor(props) {
    super(props);

    this.renderList = this.renderList.bind(this);
    this.buildHeaders = this.buildHeaders.bind(this);
    this.buildColumns = this.buildColumns.bind(this);
    this.buildDataSources = this.buildDataSources.bind(this);
    this.onPermissionChange = this.onPermissionChange.bind(this);
    this.buildGenericPermissionsPanel = this.buildGenericPermissionsPanel.bind(this);
    this.applyPermissions = this.applyPermissions.bind(this);
    // this.handleAddRole = this.handleAddRole.bind(this);

    this.handleCollapseAll = this.handleCollapseAll.bind(this);
    this.handleExpandAll = this.handleExpandAll.bind(this);
    this.hanldeCollapseOnChange = this.hanldeCollapseOnChange.bind(this);

    this.state = {
      currentActiveKeys: ['1']
    };
  }

  componentDidMount() {
    this.props.GetPermissions();
  }

  handleExpandAll() {
    this.setState({
      currentActiveKeys: ['1', '2', '3', '4', '5', '6', '7', '8']
    });
  }

  handleCollapseAll() {
    this.setState({
      currentActiveKeys: ['']
    });
  }

  hanldeCollapseOnChange(key) {
    this.setState({
      currentActiveKeys: key
    });
  }

  renderList() {
    return (
      <div className="user-profile--roles-and-permissions">
        <div
          className="user-profile--roles-and-permissions--list-of-permissions">

          <div
            className={'user-profile--roles-and-permissions--list-of-permissions--header'}>
            <div
              className={'user-profile--roles-and-permissions--list-of-permissions--actions'}>
              Actions
              <div
                className={'user-profile--roles-and-permissions--list-of-permissions--actions-links'}>
                <a href='#' onClick={this.handleCollapseAll}>
                  Collapse all
                </a>
                <a href='#' onClick={this.handleExpandAll}>
                  Expand all
                </a>
              </div>
            </div>
            {this.buildHeaders()}
          </div>

          <div className="list-of-permissions-collapsed">
            <Collapse className="no-styles"
                      onChange={this.hanldeCollapseOnChange}
                      activeKey={this.state.currentActiveKeys}>
              {this.buildGenericPermissionsPanel(5, 'Permissions Control', 17, 4)}
              {this.buildGenericPermissionsPanel(3, 'Users', 9, 4)}
              {this.buildGenericPermissionsPanel(7, 'Own Devices', 26, 5)}
              {this.buildGenericPermissionsPanel(6, 'Organization Devices', 21, 5)}
              {this.buildGenericPermissionsPanel(8, 'Auth Token', 31, 1)}
              {this.buildGenericPermissionsPanel(1, 'Blynk.Air: Firmware Over-The-Air Updates', 0, 5)}
              {this.buildGenericPermissionsPanel(4, 'Products', 13, 4)}
              {this.buildGenericPermissionsPanel(2, 'Organizations', 5, 4)}
            </Collapse>
          </div>
        </div>
      </div>
    );
  }

  onPermissionChange(role, index, value) {
    const {
      id,
      name,
      permissionGroup2,
    } = role;


    this.props.UpdateRole({
      id,
      name,
      permissionGroup1: this.applyPermissions(role, index, value),
      permissionGroup2,
    });
  }

  applyPermissions(
    { permissionGroup1, permissionsGroup1Binary },
    index,
    value
  ) {
    const newPermissions = Math.pow(2, index + 1);
    let result = permissionGroup1 ^ newPermissions;

    if (!value) {
      for (let perm of PERMISSIONS_TABLE[index + 1].dependentPermsToRemove) {
        if (permissionsGroup1Binary[permissionsGroup1Binary.length - perm - 1] === '1') {
          result = result ^ Math.pow(2, perm);
        }
      }
    }
    else {
      for (let perm of PERMISSIONS_TABLE[index + 1].dependentPermsToActivate) {
        if (permissionsGroup1Binary[permissionsGroup1Binary.length - perm - 1] === '0') {
          result = result ^ Math.pow(2, perm);
        }
      }
    }

    return result;
  }

  buildGenericPermissionsPanel(key, header, startingPermission, offset) {
    return (
      <Panel header={<div>
        <LinearIcon
          type={this.state.currentActiveKeys.indexOf(key.toString()) < 0 ? "plus-square" : "minus-square"}/> {header}
      </div>} key={key} className='list-of-permissions-collapsed-panel'>
        <div className="list-of-permissions-items--content">
          <div className={'roles-list--role--table-border'}></div>
          <Table showHeader={false}
                 pagination={false}
                 dataSource={this.buildDataSources(startingPermission, offset)}
                 columns={this.buildColumns()}/>
        </div>
      </Panel>
    );
  }

  buildDataSources(startingPermission, offset) {
    const result = [];

    for (let i = 0; i != offset; i++) {
      let currentIndex = startingPermission + i;
      let value = {
        key: currentIndex,
        name: PERMISSIONS_TABLE[currentIndex].value,
      };

      for (let role of this.props.roles) {
        const { name, permissionsGroup1Binary } = role;

        value[name.toLowerCase().trim()] = {
          role,
          index: currentIndex - 1,
          value: Boolean(Number(permissionsGroup1Binary[permissionsGroup1Binary.length - currentIndex - 1])),
        };
      }

      result.push(value);
    }

    return result;
  }

  buildColumns() {
    const result = [
      {
        dataIndex: 'name',
        key: 'name',
        width: '292px',
        style: { borderTop: '1px solid #e9e9e9;' },
        render: value => <div
          className='permissions-table-main-column'>{value}</div>,
      }
    ];

    for (let i = 0; i < this.props.roles.length; i++) {
      let role = this.props.roles[i];
      let value = {
        dataIndex: role.name.toLowerCase().trim(),
        key: role.name.toLowerCase().trim(),
        render: value => <Switch className='permissions-table-switch'
                                 size="small"
                                 checked={value.value} onChange={
          checked => this.onPermissionChange(value.role, value.index, checked)}/>,
      };

      if (i + 1 !== this.props.roles.length) {
        value.width = '184px';
      }

      result.push(value);
    }

    return result;
  }

  buildHeaders() {
    return this.props.roles.map((role) => {
        return (<div
          key={role.name}
          className={'user-profile--roles-and-permissions--list-of-permissions--role'}>
          <div
            className={'user-profile--roles-and-permissions--list-of-permissions--role-header'}>
            {role.name}
          </div>
          <div
            className={'user-profile--roles-and-permissions--list-of-permissions--users-count'}>
            0 USERS
          </div>
        </div>);
      }
    );
  }

  render() {

    return (
      this.renderList()
    );
  }

}

export default RolesAndPermissions;
