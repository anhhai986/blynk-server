import React                                from 'react';
import {
  Button,
  Tabs,
  // Icon,
  // Popover
}                                           from 'antd';
import {MainLayout}                         from 'components';
import {
  TABS,
  // FORMS,
  // PRODUCT_CREATE_INITIAL_VALUES,
}                                           from 'services/Products';
import {
  Info        as InfoTab,
  // Events      as EventsTab,
  // Metadata    as MetadataTab,
  // DataStreams as DataStreamsTab,
}                                           from '../ProductManage';

// import DashboardTab                         from 'scenes/Products/scenes/Dashboard';
// import MetadataIntroductionMessage          from '../MetadataIntroductionMessage';

import {
  // HARDWARES,
  // CONNECTIONS_TYPES,
  AVAILABLE_HARDWARE_TYPES_LIST,
  AVAILABLE_CONNECTION_TYPES_LIST,
} from 'services/Devices';


import PropTypes from 'prop-types';

import ImmutablePropTypes from 'react-immutable-proptypes';

import {
  reduxForm
} from 'redux-form';

@reduxForm()
class ProductCreate extends React.Component {

  static contextTypes = {
    router: React.PropTypes.object
  };

  static propTypes = {

    formValues: ImmutablePropTypes.contains({
      name: PropTypes.string,
      boardType: PropTypes.oneOf(AVAILABLE_HARDWARE_TYPES_LIST),
      connectionType: PropTypes.oneOf(AVAILABLE_CONNECTION_TYPES_LIST),
      description: PropTypes.string,
      logoUrl: PropTypes.string,
    }),

    loading: PropTypes.bool,
    invalid: PropTypes.bool,
    submitting: PropTypes.bool,

    onCancel: PropTypes.func,
    onSubmit: PropTypes.func,
    handleSubmit: PropTypes.func,

    // handleCancel: React.PropTypes.func,
    // handleSubmit: React.PropTypes.func,
    // onInfoValuesChange: React.PropTypes.func,
    // onEventsFieldsChange: React.PropTypes.func,
    // onMetadataFieldChange: React.PropTypes.func,
    // onMetadataFieldsChange: React.PropTypes.func,
    // onDataStreamsFieldChange: React.PropTypes.func,
    // onDataStreamsFieldsChange: React.PropTypes.func,
    // updateMetadataFirstTimeFlag: React.PropTypes.func,
    //
    // isMetadataInfoRead: React.PropTypes.bool,
    // isInfoFormInvalid: React.PropTypes.bool,
    // isEventsFormInvalid: React.PropTypes.bool,
    // isMetadataFormInvalid: React.PropTypes.bool,
    // isDataStreamsFormInvalid: React.PropTypes.bool,
    //
    // params: React.PropTypes.object,
    // product: React.PropTypes.object,
    // loading: React.PropTypes.bool,
  };

  constructor(props) {
    super(props);

    // this.state = {
    //   originalName: null,
    //   submited: false,
    //   activeTab: props.params.tab || TABS.INFO,
    //   metadataIntroVisible: false
    // };

    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleCancel = this.handleCancel.bind(this);

    // this.handleTabChange = this.handleTabChange.bind(this);
    // this.toggleMetadataIntroductionMessage = this.toggleMetadataIntroductionMessage.bind(this);

  }

  componentWillMount() {
    // if (!this.state.originalName) {
    //   this.setState({
    //     originalName: this.props.product.info.values.name
    //   });
    // }
  }

  TABS = {
    INFO: 'info',
    METADATA: 'metadata',
    // DATA_STREAMS: 'datastreams',
    // EVENTS: 'events'
  };

  // isMetadataIntroductionMessageVisible() {
  //   if (!this.props.isMetadataInfoRead) return true;
  //
  //   return this.state.metadataIntroVisible;
  // }


  // toggleMetadataIntroductionMessage() {
  //
  //   this.setState({
  //     metadataIntroVisible: !this.state.metadataIntroVisible,
  //   });
  //
  //   if (!this.props.isMetadataInfoRead) {
  //     this.props.updateMetadataFirstTimeFlag(false);
  //     this.setState({
  //       metadataIntroVisible: false
  //     });
  //   }
  // }

  // handleTabChange(key) {
  //   this.setState({
  //     activeTab: key
  //   });
  // }

  // isInfoFormInvalid() {
  //   return this.props.isInfoFormInvalid;
  // }
  //
  // productInfoInvalidIcon() {
  //   return this.state.submited && this.isInfoFormInvalid() &&
  //     <Icon type="exclamation-circle-o" className="product-tab-invalid"/> || null;
  // }
  //
  // productDataStreamsInvalidIcon() {
  //   return this.state.submited && this.props.isDataStreamsFormInvalid &&
  //     <Icon type="exclamation-circle-o" className="product-tab-invalid"/> || null;
  // }
  //
  // productMetadataInvalidIcon() {
  //   return this.state.submited && this.props.isMetadataFormInvalid &&
  //     <Icon type="exclamation-circle-o" className="product-tab-invalid"/> || null;
  // }
  //
  // productEventsInvalidIcon() {
  //   return this.state.submited && this.props.isEventsFormInvalid &&
  //     <Icon type="exclamation-circle-o" className="product-tab-invalid"/> || null;
  // }

  handleCancel() {
    this.props.onCancel();
  }

  handleSubmit() {
    this.props.onSubmit();
  }

  render() {
    return (
      <MainLayout>
        <MainLayout.Header title={this.props.formValues.get('name') || 'New Product'}
                           options={(
                             <div>
                               <Button type="default"
                                       onClick={this.handleCancel}>
                                 Cancel
                               </Button>
                               <Button type="primary"
                                       onClick={this.props.handleSubmit}
                                       loading={this.props.loading}
                                       disabled={this.props.invalid || this.props.submitting}>
                                 Create
                               </Button>
                             </div>
                           )}
        />
        <MainLayout.Content className="product-create-content">

          <Tabs defaultActiveKey={TABS.INFO} activeKey={TABS.INFO} className="products-tabs">

            <Tabs.TabPane tab={<span>Info</span>} key={TABS.INFO}>
              <InfoTab />
            </Tabs.TabPane>

          </Tabs>

        </MainLayout.Content>
      </MainLayout>
    );
  }
}

export default ProductCreate;
